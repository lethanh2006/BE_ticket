package vn.datve.dat_ve.booking;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.datve.dat_ve.seat.SeatsChangedEvent;

/**
 * Định kỳ chuyển đơn quá hạn giữ ghế sang EXPIRED và nhả ghế, trong một câu SQL.
 * Câu UPDATE chờ khoá dòng nên không đè lên một lần thanh toán đang diễn ra; chạy trên
 * nhiều instance cùng lúc cũng an toàn vì đơn đã EXPIRED không còn khớp điều kiện.
 */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    public BookingExpiryJob(JdbcClient jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-check-interval}")
    @Transactional
    public void expireStaleHolds() {
        List<Long> showtimeIds = jdbc.sql("""
                        WITH expired AS (
                            UPDATE bookings SET status = 'EXPIRED', updated_at = now()
                            WHERE status = 'PENDING' AND expires_at <= now()
                            RETURNING id, showtime_id
                        ), released AS (
                            UPDATE booking_seats bs SET reservation_status = 'RELEASED'
                            FROM expired
                            WHERE bs.booking_id = expired.id AND bs.reservation_status = 'HELD'
                            RETURNING bs.id
                        )
                        SELECT DISTINCT showtime_id FROM expired
                        """)
                .query(Long.class)
                .list();
        if (!showtimeIds.isEmpty()) {
            log.info("Đã nhả ghế của các đơn quá hạn ở {} suất diễn", showtimeIds.size());
            showtimeIds.forEach(id -> events.publishEvent(new SeatsChangedEvent(id)));
        }
    }
}
