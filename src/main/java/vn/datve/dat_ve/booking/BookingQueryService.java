package vn.datve.dat_ve.booking;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import vn.datve.dat_ve.booking.dto.BookingResponse;
import vn.datve.dat_ve.booking.dto.BookingSeatResponse;
import vn.datve.dat_ve.booking.dto.CustomerResponse;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.common.response.PageResponse;

/**
 * Phía đọc của đơn đặt vé: một câu SQL cho các đơn (kèm suất diễn, sự kiện, khách hàng)
 * và một câu cho ghế của cả trang (WHERE booking_id IN ...), tránh N+1.
 */
@Service
public class BookingQueryService {

    /** Đơn PENDING đã quá hạn giữ ghế được hiển thị là EXPIRED ngay, không đợi job dọn dẹp. */
    private static final String EFFECTIVE_STATUS =
            "CASE WHEN b.status = 'PENDING' AND b.expires_at <= now() THEN 'EXPIRED' ELSE b.status END";

    private static final String SELECT = """
            SELECT b.id, b.showtime_id, st.event_id, e.name AS event_name, e.venue,
                   st.start_time, st.end_time, %s AS status, b.total_amount, b.expires_at,
                   b.created_at, b.ticket_code, b.checked_in_at, u.id AS user_id, u.email, u.full_name
            FROM bookings b
            JOIN showtimes st ON st.id = b.showtime_id
            JOIN events e ON e.id = st.event_id
            JOIN users u ON u.id = b.user_id
            """.formatted(EFFECTIVE_STATUS);

    private final JdbcClient jdbc;

    public BookingQueryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Đơn của chính người gọi; đơn của người khác trả 404 để không lộ là đơn đó tồn tại. */
    public BookingResponse getForUser(long bookingId, long userId, boolean admin) {
        List<BookingResponse> found = load(SELECT + " WHERE b.id = :id AND (:admin OR b.user_id = :userId)",
                Map.of("id", bookingId, "admin", admin, "userId", userId), admin);
        if (found.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Không tìm thấy đơn đặt vé");
        }
        return found.getFirst();
    }

    public PageResponse<BookingResponse> pageForUser(long userId, BookingStatus status, int page, int size) {
        String where = " WHERE b.user_id = :userId AND (:status = '' OR " + EFFECTIVE_STATUS + " = :status)";
        Map<String, Object> params = Map.of("userId", userId, "status", status == null ? "" : status.name());
        return page(where, params, page, size, false);
    }

    /** Quản trị: lọc theo trạng thái, tìm theo email, họ tên (không dấu), mã vé hoặc mã đơn. */
    public PageResponse<BookingResponse> pageForAdmin(BookingStatus status, String q, int page, int size) {
        String where = """
                 WHERE (:status = '' OR %s = :status)
                   AND (:q = '' OR lower(u.email) LIKE '%%' || lower(:q) || '%%'
                        OR lower(unaccent(u.full_name)) LIKE '%%' || lower(unaccent(:q)) || '%%'
                        OR b.ticket_code = lower(:q) OR CAST(b.id AS text) = :q)
                """.formatted(EFFECTIVE_STATUS);
        Map<String, Object> params = Map.of("status", status == null ? "" : status.name(),
                "q", q == null ? "" : q.strip());
        return page(where, params, page, size, true);
    }

    private PageResponse<BookingResponse> page(String where, Map<String, Object> params, int page, int size,
            boolean withCustomer) {
        int pageSize = Math.clamp(size, 1, 50);
        int pageIndex = Math.max(page, 0);
        long total = jdbc.sql("SELECT count(*) FROM bookings b JOIN users u ON u.id = b.user_id" + where)
                .params(params).query(Long.class).single();
        Map<String, Object> pageParams = new java.util.HashMap<>(params);
        pageParams.put("limit", pageSize);
        pageParams.put("offset", (long) pageIndex * pageSize);
        List<BookingResponse> content = load(SELECT + where + " ORDER BY b.id DESC LIMIT :limit OFFSET :offset",
                pageParams, withCustomer);
        return new PageResponse<>(content, pageIndex, pageSize, total, (int) Math.ceil((double) total / pageSize));
    }

    private List<BookingResponse> load(String sql, Map<String, Object> params, boolean withCustomer) {
        List<Row> rows = jdbc.sql(sql).params(params).query((rs, rowNum) -> Row.map(rs)).list();
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<BookingSeatResponse>> seats = jdbc.sql("""
                        SELECT bs.booking_id, bs.seat_id, s.seat_code, s.category, bs.unit_price, bs.reservation_status
                        FROM booking_seats bs
                        JOIN seats s ON s.id = bs.seat_id
                        WHERE bs.booking_id IN (:ids)
                        ORDER BY s.id
                        """)
                .param("ids", rows.stream().map(Row::id).toList())
                .query((rs, rowNum) -> Map.entry(rs.getLong("booking_id"), new BookingSeatResponse(
                        rs.getLong("seat_id"), rs.getString("seat_code"), rs.getString("category"),
                        rs.getBigDecimal("unit_price"), BookingSeatStatus.valueOf(rs.getString("reservation_status")))))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        List<BookingResponse> result = new ArrayList<>();
        for (Row row : rows) {
            result.add(row.toResponse(seats.getOrDefault(row.id(), List.of()), withCustomer));
        }
        return result;
    }

    private record Row(long id, long showtimeId, long eventId, String eventName, String venue, Instant startTime,
            Instant endTime, BookingStatus status, BigDecimal totalAmount, Instant expiresAt, Instant createdAt,
            String ticketCode, Instant checkedInAt, CustomerResponse customer) {

        static Row map(ResultSet rs) throws SQLException {
            return new Row(rs.getLong("id"), rs.getLong("showtime_id"), rs.getLong("event_id"),
                    rs.getString("event_name"), rs.getString("venue"), instant(rs, "start_time"),
                    instant(rs, "end_time"), BookingStatus.valueOf(rs.getString("status")),
                    rs.getBigDecimal("total_amount"), instant(rs, "expires_at"), instant(rs, "created_at"),
                    rs.getString("ticket_code"), instant(rs, "checked_in_at"),
                    new CustomerResponse(rs.getLong("user_id"), rs.getString("email"), rs.getString("full_name")));
        }

        BookingResponse toResponse(List<BookingSeatResponse> seats, boolean withCustomer) {
            return new BookingResponse(id, showtimeId, eventId, eventName, venue, startTime, endTime, status, seats,
                    totalAmount, expiresAt, createdAt, ticketCode, checkedInAt, null, null, null, null,
                    withCustomer ? customer : null);
        }

        private static Instant instant(ResultSet rs, String column) throws SQLException {
            OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
            return value == null ? null : value.toInstant();
        }
    }
}
