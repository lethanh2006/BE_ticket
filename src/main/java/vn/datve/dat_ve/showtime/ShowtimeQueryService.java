package vn.datve.dat_ve.showtime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.showtime.dto.ShowtimeResponse;

/** Suất diễn kèm tổng số ghế, số ghế còn trống và giá thấp nhất, tính trong một câu SQL. */
@Service
public class ShowtimeQueryService {

    /** Ghế bị chiếm khi đã bán, hoặc đang được giữ bởi một đơn chưa hết hạn. */
    public static final String SEAT_TAKEN = """
            EXISTS (
                SELECT 1 FROM booking_seats bs
                JOIN bookings b ON b.id = bs.booking_id
                WHERE bs.seat_id = s.id
                  AND (bs.reservation_status = 'BOOKED'
                       OR (bs.reservation_status = 'HELD' AND b.expires_at > now())))
            """;

    private static final String SELECT = """
            SELECT st.id, st.event_id, st.start_time, st.end_time,
                   count(s.id) AS total_seats,
                   count(s.id) FILTER (WHERE NOT %s) AS available_seats,
                   min(s.face_value) AS min_price
            FROM showtimes st
            LEFT JOIN seats s ON s.showtime_id = st.id
            """.formatted(SEAT_TAKEN);

    private final JdbcClient jdbc;

    public ShowtimeQueryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ShowtimeResponse> findByEventId(long eventId) {
        boolean eventExists = jdbc.sql("SELECT EXISTS (SELECT 1 FROM events WHERE id = :id)")
                .param("id", eventId).query(Boolean.class).single();
        if (!eventExists) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Không tìm thấy sự kiện có id " + eventId);
        }
        return jdbc.sql(SELECT + " WHERE st.event_id = :eventId GROUP BY st.id ORDER BY st.start_time")
                .param("eventId", eventId)
                .query((rs, rowNum) -> map(rs))
                .list();
    }

    public ShowtimeResponse get(long showtimeId) {
        return jdbc.sql(SELECT + " WHERE st.id = :id GROUP BY st.id")
                .param("id", showtimeId)
                .query((rs, rowNum) -> map(rs))
                .optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHOWTIME_NOT_FOUND",
                        "Không tìm thấy suất diễn có id " + showtimeId));
    }

    private static ShowtimeResponse map(ResultSet rs) throws SQLException {
        return new ShowtimeResponse(rs.getLong("id"), rs.getLong("event_id"),
                rs.getObject("start_time", OffsetDateTime.class).toInstant(),
                rs.getObject("end_time", OffsetDateTime.class).toInstant(),
                rs.getLong("total_seats"), rs.getLong("available_seats"), rs.getBigDecimal("min_price"),
                null, null);
    }
}
