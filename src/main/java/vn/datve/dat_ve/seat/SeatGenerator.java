package vn.datve.dat_ve.seat;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.seat.dto.SeatSectionInput;

/** Sinh ghế cho suất diễn: mỗi khu vực là một câu INSERT ... SELECT (hàng × số ghế), không lặp từng ghế. */
@Component
public class SeatGenerator {

    private final JdbcClient jdbc;

    public SeatGenerator(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public int addSections(long showtimeId, List<SeatSectionInput> sections) {
        int created = 0;
        try {
            for (SeatSectionInput section : sections) {
                created += jdbc.sql("""
                                INSERT INTO seats (showtime_id, seat_code, category, face_value)
                                SELECT :showtimeId, r.code || n, :category, :price
                                FROM unnest(string_to_array(:rows, ',')) AS r(code)
                                CROSS JOIN generate_series(1, :perRow) AS n
                                ORDER BY r.code, n
                                """)
                        .param("showtimeId", showtimeId)
                        .param("category", section.category().strip())
                        .param("price", section.faceValue())
                        .param("rows", String.join(",", section.rows()))
                        .param("perRow", section.seatsPerRow())
                        .update();
            }
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "SEAT_CODE_EXISTS", "Suất diễn đã có ghế trùng mã (ví dụ A1)");
        }
        return created;
    }
}
