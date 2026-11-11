package vn.datve.dat_ve.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import vn.datve.dat_ve.admin.dto.StatsResponse;
import vn.datve.dat_ve.booking.BookingStatus;

/** Số liệu tổng quan cho trang quản trị, tính bằng SQL tổng hợp. */
@Service
public class AdminStatsService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DAYS = 14;

    private final JdbcClient jdbc;

    public AdminStatsService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public StatsResponse stats() {
        Map<String, Object> totals = jdbc.sql("""
                        SELECT (SELECT count(*) FROM events) AS events,
                               (SELECT count(*) FROM showtimes WHERE start_time > now()) AS upcoming_showtimes,
                               (SELECT count(*) FROM users) AS users,
                               (SELECT count(*) FROM booking_seats WHERE reservation_status = 'BOOKED') AS seats_sold,
                               (SELECT COALESCE(sum(total_amount), 0) FROM bookings WHERE status = 'CONFIRMED') AS revenue
                        """)
                .query().singleRow();

        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            byStatus.put(status, 0L);
        }
        jdbc.sql("SELECT status, count(*) AS total FROM bookings GROUP BY status")
                .query((rs, rowNum) -> Map.entry(BookingStatus.valueOf(rs.getString("status")), rs.getLong("total")))
                .list()
                .forEach(entry -> byStatus.put(entry.getKey(), entry.getValue()));

        // Doanh thu theo ngày (giờ Việt Nam) của 14 ngày gần nhất; generate_series để ngày không có đơn vẫn hiện 0
        LocalDate today = LocalDate.now(VIETNAM);
        List<StatsResponse.Daily> daily = jdbc.sql("""
                        SELECT d::date AS day, count(p.id) AS confirmed, COALESCE(sum(p.amount), 0) AS revenue
                        FROM generate_series(CAST(:from AS date), CAST(:to AS date), interval '1 day') AS d
                        LEFT JOIN payments p
                               ON p.status = 'SUCCESS'
                              AND (p.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date = d::date
                        GROUP BY d
                        ORDER BY d
                        """)
                .param("from", today.minusDays(DAYS - 1))
                .param("to", today)
                .query((rs, rowNum) -> new StatsResponse.Daily(rs.getObject("day", LocalDate.class),
                        rs.getLong("confirmed"), rs.getBigDecimal("revenue")))
                .list();

        List<StatsResponse.TopEvent> topEvents = jdbc.sql("""
                        SELECT e.id, e.name, count(bs.id) AS seats_sold, COALESCE(sum(bs.unit_price), 0) AS revenue
                        FROM events e
                        JOIN showtimes st ON st.event_id = e.id
                        JOIN booking_seats bs ON bs.showtime_id = st.id AND bs.reservation_status = 'BOOKED'
                        GROUP BY e.id, e.name
                        ORDER BY revenue DESC
                        LIMIT 5
                        """)
                .query((rs, rowNum) -> new StatsResponse.TopEvent(rs.getLong("id"), rs.getString("name"),
                        rs.getLong("seats_sold"), rs.getBigDecimal("revenue")))
                .list();

        return new StatsResponse(number(totals, "events"), number(totals, "upcoming_showtimes"),
                number(totals, "users"), byStatus, number(totals, "seats_sold"), (BigDecimal) totals.get("revenue"),
                daily, topEvents);
    }

    private static long number(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
    }
}
