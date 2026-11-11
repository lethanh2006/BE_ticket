package vn.datve.dat_ve;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Gọi API qua HTTP thật (Tomcat cổng ngẫu nhiên) trên PostgreSQL thật (Testcontainers). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ApiIntegrationTests {

    @Value("${local.server.port}")
    int port;

    @Autowired
    JsonMapper json;

    @Autowired
    JdbcClient jdbc;

    RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {
                })
                .build();
    }

    @Test
    void registerLoginAndReadProfile() {
        String email = uniqueEmail();
        Res registered = call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("email", email, "password", "Matkhau@123", "fullName", "Trần Thị Mai"));
        Res login = call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email.toUpperCase(), "password", "Matkhau@123"));
        Res me = call(HttpMethod.GET, "/api/auth/me", login.text("accessToken"), null);
        Res wrongPassword = call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "password", "sai-mat-khau"));
        Res duplicate = call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("email", email, "password", "Matkhau@123", "fullName", "Trùng"));

        assertThat(registered.status()).isEqualTo(201);
        assertThat(registered.body().get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(me.body().get("fullName").asString()).isEqualTo("Trần Thị Mai");
        assertThat(me.body().get("role").asString()).isEqualTo("USER");
        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(wrongPassword.code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(duplicate.code()).isEqualTo("EMAIL_ALREADY_USED");
    }

    @Test
    void tokensAndRolesAreEnforced() {
        String user = register();

        assertThat(call(HttpMethod.GET, "/api/bookings", null, null).code()).isEqualTo("UNAUTHENTICATED");
        assertThat(call(HttpMethod.GET, "/api/bookings", "not.a.jwt", null).code()).isEqualTo("INVALID_TOKEN");
        Res forbidden = call(HttpMethod.GET, "/api/admin/stats", user, null);
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.code()).isEqualTo("FORBIDDEN");
        assertThat(call(HttpMethod.GET, "/api/events", null, null).status()).isEqualTo(200);
    }

    @Test
    void bookPayReplayAndCheckIn() {
        String admin = admin();
        String user = register();
        long showtimeId = createShowtime(admin, "Đêm nhạc kiểm thử", 5);
        List<Long> seats = seatIds(showtimeId);

        Res booking = call(HttpMethod.POST, "/api/showtimes/" + showtimeId + "/bookings", user,
                Map.of("seatIds", List.of(seats.get(0), seats.get(1))));
        long bookingId = booking.body().get("id").asLong();
        assertThat(booking.status()).isEqualTo(201);
        assertThat(booking.body().get("status").asString()).isEqualTo("PENDING");
        assertThat(seatStatus(showtimeId, seats.get(0))).isEqualTo("HELD");

        String key = UUID.randomUUID().toString();
        Res paid = pay(user, bookingId, key);
        Res replay = pay(user, bookingId, key);
        Res secondPayment = pay(user, bookingId, UUID.randomUUID().toString());

        assertThat(paid.status()).isEqualTo(201);
        assertThat(paid.body().get("status").asString()).isEqualTo("SUCCESS");
        assertThat(paid.body().get("bookingStatus").asString()).isEqualTo("CONFIRMED");
        assertThat(replay.status()).isEqualTo(200);
        assertThat(replay.headers().getFirst("Idempotent-Replayed")).isEqualTo("true");
        assertThat(replay.body().get("id").asLong()).isEqualTo(paid.body().get("id").asLong());
        assertThat(secondPayment.code()).isEqualTo("BOOKING_NOT_PAYABLE");
        assertThat(paymentCount(bookingId)).isEqualTo(1);
        assertThat(seatStatus(showtimeId, seats.get(0))).isEqualTo("BOOKED");

        String ticketCode = call(HttpMethod.GET, "/api/bookings/" + bookingId, user, null).text("ticketCode");
        assertThat(ticketCode).hasSize(16);
        Res checkIn = call(HttpMethod.POST, "/api/admin/bookings/check-in", admin, Map.of("ticketCode", ticketCode));
        Res again = call(HttpMethod.POST, "/api/admin/bookings/check-in", admin, Map.of("ticketCode", ticketCode));
        assertThat(checkIn.text("result")).isEqualTo("CHECKED_IN");
        assertThat(again.text("result")).isEqualTo("ALREADY_CHECKED_IN");

        Res mine = call(HttpMethod.GET, "/api/bookings?status=CONFIRMED", user, null);
        assertThat(mine.body().get("content").get(0).get("id").asLong()).isEqualTo(bookingId);
        Res stranger = call(HttpMethod.GET, "/api/bookings/" + bookingId, register(), null);
        assertThat(stranger.status()).isEqualTo(404);
    }

    @Test
    void onlyOneOfManyConcurrentBookingsGetsTheSeat() throws Exception {
        String admin = admin();
        long showtimeId = createShowtime(admin, "Suất tranh ghế", 3);
        long seatId = seatIds(showtimeId).getFirst();
        List<String> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            users.add(register());
        }

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Res>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(users.size())) {
            for (String user : users) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return call(HttpMethod.POST, "/api/showtimes/" + showtimeId + "/bookings", user,
                            Map.of("seatIds", List.of(seatId)));
                }));
            }
            start.countDown();
        }
        List<Res> results = new ArrayList<>();
        for (Future<Res> future : futures) {
            results.add(future.get());
        }

        assertThat(results).filteredOn(r -> r.status() == 201).hasSize(1);
        assertThat(results).filteredOn(r -> r.status() == 409).hasSize(9)
                .allSatisfy(r -> assertThat(r.code()).isEqualTo("SEAT_UNAVAILABLE"));
    }

    @Test
    void cancellingAPendingBookingReleasesItsSeats() {
        String admin = admin();
        String user = register();
        long showtimeId = createShowtime(admin, "Suất huỷ vé", 2);
        long seatId = seatIds(showtimeId).getFirst();
        long bookingId = call(HttpMethod.POST, "/api/showtimes/" + showtimeId + "/bookings", user,
                Map.of("seatIds", List.of(seatId))).body().get("id").asLong();

        Res cancelled = call(HttpMethod.POST, "/api/bookings/" + bookingId + "/cancel", user, null);

        assertThat(cancelled.text("status")).isEqualTo("CANCELLED");
        assertThat(seatStatus(showtimeId, seatId)).isEqualTo("AVAILABLE");
        assertThat(call(HttpMethod.POST, "/api/showtimes/" + showtimeId + "/bookings", register(),
                Map.of("seatIds", List.of(seatId))).status()).isEqualTo(201);
    }

    @Test
    void searchIgnoresVietnameseAccentsAndShowsNextShowtimeAndMinPrice() {
        String admin = admin();
        createShowtime(admin, "Lễ hội Đường phố Kiểm thử", 4);

        // RestClient tự mã hoá URI: truyền dấu cách thật, không tự viết %20 (sẽ bị mã hoá thành %2520)
        Res found = call(HttpMethod.GET, "/api/events?q=le hoi duong pho kiem", null, null);

        JsonNode first = found.body().get("content").get(0);
        assertThat(found.body().get("totalElements").asLong()).isEqualTo(1);
        assertThat(first.get("name").asString()).isEqualTo("Lễ hội Đường phố Kiểm thử");
        assertThat(first.get("nextShowtime").isNull()).isFalse();
        assertThat(first.get("minPrice").asLong()).isEqualTo(100000);
    }

    @Test
    void overlappingShowtimesOfOneEventAreRejectedByTheDatabase() {
        String admin = admin();
        long eventId = createEvent(admin, "Sự kiện chống trùng giờ");
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Map<String, Object> first = Map.of("startTime", start.toString(), "endTime", start.plus(2, ChronoUnit.HOURS).toString());
        Map<String, Object> overlapping = Map.of("startTime", start.plus(1, ChronoUnit.HOURS).toString(),
                "endTime", start.plus(3, ChronoUnit.HOURS).toString());

        assertThat(call(HttpMethod.POST, "/api/admin/events/" + eventId + "/showtimes", admin, first).status()).isEqualTo(201);
        Res rejected = call(HttpMethod.POST, "/api/admin/events/" + eventId + "/showtimes", admin, overlapping);
        assertThat(rejected.status()).isEqualTo(409);
        assertThat(rejected.code()).isEqualTo("SHOWTIME_OVERLAP");
    }

    @Test
    void adminStatsCountsAllBookingStatuses() {
        Res stats = call(HttpMethod.GET, "/api/admin/stats", admin(), null);

        assertThat(stats.status()).isEqualTo(200);
        assertThat(stats.body().get("bookingsByStatus").size()).isEqualTo(5);
        assertThat(stats.body().get("daily").size()).isEqualTo(14);
    }

    // ---- helpers ----

    record Res(int status, JsonNode body, HttpHeaders headers) {

        String code() {
            return body.get("code").asString();
        }

        String text(String field) {
            return body.get(field).asString();
        }
    }

    private Res call(HttpMethod method, String path, String token, Object body) {
        return call(method, path, token, body, Map.of());
    }

    private Res call(HttpMethod method, String path, String token, Object body, Map<String, String> headers) {
        RestClient.RequestBodySpec request = http.method(method).uri(path).headers(h -> {
            if (token != null) {
                h.setBearerAuth(token);
            }
            headers.forEach(h::set);
        });
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        ResponseEntity<String> response = request.retrieve().toEntity(String.class);
        String raw = response.getBody();
        return new Res(response.getStatusCode().value(), raw == null ? null : json.readTree(raw), response.getHeaders());
    }

    private Res pay(String token, long bookingId, String key) {
        return call(HttpMethod.POST, "/api/bookings/" + bookingId + "/payments", token, Map.of(),
                Map.of("Idempotency-Key", key));
    }

    private static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 12) + "@test.local";
    }

    private String register() {
        return call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("email", uniqueEmail(), "password", "Matkhau@123", "fullName", "Khách Kiểm Thử")).text("accessToken");
    }

    private String admin() {
        String email = uniqueEmail();
        call(HttpMethod.POST, "/api/auth/register", null, Map.of("email", email, "password", "Matkhau@123", "fullName", "Quản trị"));
        jdbc.sql("UPDATE users SET role = 'ADMIN' WHERE email = ?").param(email).update();
        return call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "password", "Matkhau@123")).text("accessToken");
    }

    private long createEvent(String admin, String name) {
        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("venue", "Nhà hát Kiểm thử");
        event.put("category", "Kiểm thử");
        Res created = call(HttpMethod.POST, "/api/admin/events", admin, event);
        assertThat(created.status()).isEqualTo(201);
        return created.body().get("id").asLong();
    }

    /** Sự kiện mới + suất diễn sau 2 ngày + một hàng ghế giá 100.000. */
    private long createShowtime(String admin, String eventName, int seats) {
        long eventId = createEvent(admin, eventName);
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Res showtime = call(HttpMethod.POST, "/api/admin/events/" + eventId + "/showtimes", admin, Map.of(
                "startTime", start.toString(),
                "endTime", start.plus(2, ChronoUnit.HOURS).toString(),
                "sections", List.of(Map.of("category", "STANDARD", "rows", List.of("A"), "seatsPerRow", seats,
                        "faceValue", 100000))));
        assertThat(showtime.status()).isEqualTo(201);
        assertThat(showtime.body().get("totalSeats").asLong()).isEqualTo(seats);
        return showtime.body().get("id").asLong();
    }

    private List<Long> seatIds(long showtimeId) {
        List<Long> ids = new ArrayList<>();
        call(HttpMethod.GET, "/api/showtimes/" + showtimeId + "/seats", null, null).body()
                .forEach(seat -> ids.add(seat.get("id").asLong()));
        return ids;
    }

    private String seatStatus(long showtimeId, long seatId) {
        for (JsonNode seat : call(HttpMethod.GET, "/api/showtimes/" + showtimeId + "/seats", null, null).body()) {
            if (seat.get("id").asLong() == seatId) {
                return seat.get("status").asString();
            }
        }
        throw new IllegalArgumentException("seat " + seatId);
    }

    private long paymentCount(long bookingId) {
        return jdbc.sql("SELECT count(*) FROM payments WHERE booking_id = ?").param(bookingId).query(Long.class).single();
    }
}
