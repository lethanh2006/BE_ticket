package vn.datve.dat_ve.demo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vn.datve.dat_ve.auth.Role;
import vn.datve.dat_ve.auth.User;
import vn.datve.dat_ve.auth.UserRepository;
import vn.datve.dat_ve.event.Event;
import vn.datve.dat_ve.event.EventRepository;
import vn.datve.dat_ve.seat.SeatGenerator;
import vn.datve.dat_ve.seat.dto.SeatSectionInput;
import vn.datve.dat_ve.showtime.Showtime;
import vn.datve.dat_ve.showtime.ShowtimeRepository;

/**
 * Dữ liệu mẫu cho bản demo (profile "demo"): tài khoản quản trị và khách, 8 sự kiện có suất diễn và ghế.
 * Mỗi ngày bổ sung suất diễn mới cho sự kiện sắp hết suất để bản demo trực tuyến luôn có vé để đặt.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalTime SHOW_TIME = LocalTime.of(19, 30);

    private record DemoEvent(String name, String category, String venue, long basePrice, List<Integer> days,
            String image, String description) {
    }

    private static final List<DemoEvent> EVENTS = List.of(
            new DemoEvent("Đêm nhạc Trịnh Công Sơn", "Âm nhạc", "Nhà hát Hòa Bình, TP.HCM", 350_000, List.of(3, 4, 10), "trinh",
                    "Những ca khúc bất hủ của nhạc sĩ Trịnh Công Sơn qua giọng hát của các nghệ sĩ hàng đầu, cùng dàn nhạc giao hưởng sống."),
            new DemoEvent("Hà Nội Jazz Night", "Âm nhạc", "Nhà hát Lớn Hà Nội", 450_000, List.of(5, 12), "jazz",
                    "Một đêm jazz đầy cảm xúc với các nghệ sĩ trong nước và quốc tế, không gian sang trọng giữa lòng thủ đô."),
            new DemoEvent("Hồ Thiên Nga - Ballet", "Sân khấu", "Nhà hát Thành phố, TP.HCM", 500_000, List.of(7, 8, 14), "ballet",
                    "Vở ballet kinh điển của Tchaikovsky do đoàn ballet quốc gia trình diễn trên sân khấu lộng lẫy."),
            new DemoEvent("Kịch nói: Hồn Trương Ba, da hàng thịt", "Sân khấu", "Nhà hát Kịch Sài Gòn", 250_000, List.of(4, 11), "kich",
                    "Tác phẩm kinh điển của Lưu Quang Vũ được dàn dựng mới với góc nhìn đương đại."),
            new DemoEvent("Giải Cầu lông Mở rộng 2026", "Thể thao", "Nhà thi đấu Phú Thọ, TP.HCM", 150_000, List.of(6, 7), "caulong",
                    "Giải đấu quy tụ các tay vợt hàng đầu khu vực, các trận bán kết và chung kết hấp dẫn."),
            new DemoEvent("Hội thảo Công nghệ AI Việt Nam 2026", "Hội thảo", "Trung tâm Hội nghị GEM Center", 200_000, List.of(9), "ai",
                    "Một ngày với các diễn giả đến từ các công ty công nghệ, workshop thực hành và kết nối cộng đồng."),
            new DemoEvent("Lễ hội ẩm thực đường phố", "Lễ hội", "Công viên Tao Đàn, TP.HCM", 80_000, List.of(2, 3, 9, 10), "amthuc",
                    "Hàng trăm gian hàng ẩm thực ba miền, biểu diễn nhạc sống và khu vui chơi cho cả gia đình."),
            new DemoEvent("Stand-up Comedy: Cười Xuyên Việt", "Hài kịch", "Nhà hát Bến Thành, TP.HCM", 220_000, List.of(5, 13), "standup",
                    "Đêm diễn tập hợp những gương mặt stand-up được yêu thích nhất, đảm bảo bạn cười không ngớt."));

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatGenerator seatGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactions;
    private final JdbcClient jdbc;
    private final String adminPassword;

    public DemoDataLoader(UserRepository userRepository, EventRepository eventRepository,
            ShowtimeRepository showtimeRepository, SeatGenerator seatGenerator, PasswordEncoder passwordEncoder,
            TransactionTemplate transactions, JdbcClient jdbc, @Value("${app.demo.admin-password}") String adminPassword) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatGenerator = seatGenerator;
        this.passwordEncoder = passwordEncoder;
        this.transactions = transactions;
        this.jdbc = jdbc;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactions.executeWithoutResult(status -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User("admin@datve.local", passwordEncoder.encode(adminPassword), "Quản trị viên", Role.ADMIN));
                userRepository.save(new User("user@datve.local", passwordEncoder.encode("User@12345"), "Nguyễn Văn Khách", Role.USER));
                log.info("Đã tạo tài khoản demo admin@datve.local và user@datve.local");
            }
            if (eventRepository.count() == 0) {
                LocalDate today = LocalDate.now(VIETNAM);
                for (DemoEvent demo : EVENTS) {
                    Event event = eventRepository.save(new Event(demo.name(), demo.description(), demo.venue(),
                            "https://picsum.photos/seed/" + demo.image() + "/800/450", demo.category()));
                    for (int day : demo.days()) {
                        addShowtime(event.getId(), today.plusDays(day), demo.basePrice());
                    }
                }
                log.info("Đã tạo {} sự kiện demo", EVENTS.size());
            }
        });
        topUpShowtimes();
    }

    /** Sự kiện nào không còn suất trong 3 ngày tới thì thêm một suất sau 7 ngày. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void topUpShowtimes() {
        transactions.executeWithoutResult(status -> {
            List<Long> eventIds = jdbc.sql("""
                            SELECT e.id FROM events e
                            WHERE NOT EXISTS (SELECT 1 FROM showtimes st
                                              WHERE st.event_id = e.id AND st.start_time > now() + interval '3 days')
                            """)
                    .query(Long.class).list();
            LocalDate target = LocalDate.now(VIETNAM).plusDays(7);
            for (long eventId : eventIds) {
                BigDecimal basePrice = jdbc.sql("""
                                SELECT COALESCE(min(s.face_value), 200000) FROM seats s
                                JOIN showtimes st ON st.id = s.showtime_id WHERE st.event_id = :id
                                """)
                        .param("id", eventId).query(BigDecimal.class).single();
                addShowtime(eventId, target, basePrice.longValue());
            }
            if (!eventIds.isEmpty()) {
                log.info("Đã thêm suất diễn mới cho {} sự kiện demo", eventIds.size());
            }
        });
    }

    private void addShowtime(long eventId, LocalDate date, long basePrice) {
        Instant start = date.atTime(SHOW_TIME).atZone(VIETNAM).toInstant();
        Showtime showtime = showtimeRepository.save(new Showtime(eventId, start, start.plus(150, ChronoUnit.MINUTES)));
        // VIP gấp 3, tiêu chuẩn gấp 2, phổ thông bằng giá cơ bản
        seatGenerator.addSections(showtime.getId(), List.of(
                new SeatSectionInput("VIP", List.of("A", "B"), 10, BigDecimal.valueOf(basePrice * 3)),
                new SeatSectionInput("STANDARD", List.of("C", "D", "E", "F"), 12, BigDecimal.valueOf(basePrice * 2)),
                new SeatSectionInput("ECONOMY", List.of("G", "H"), 14, BigDecimal.valueOf(basePrice))));
    }
}
