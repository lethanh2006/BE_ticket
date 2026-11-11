package vn.datve.dat_ve.event;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.common.response.PageResponse;
import vn.datve.dat_ve.event.dto.EventSummary;

/**
 * Phía đọc của sự kiện viết bằng SQL: tìm kiếm không dấu, suất diễn sắp tới và giá thấp nhất
 * được tính bằng LATERAL join trong cùng một câu truy vấn (không N+1).
 */
@Service
public class EventQueryService {

    private static final String SELECT = """
            SELECT e.id, e.name, e.description, e.venue, e.image_url, e.category,
                   nx.next_showtime, mp.min_price
            FROM events e
            LEFT JOIN LATERAL (
                SELECT min(st.start_time) AS next_showtime
                FROM showtimes st
                WHERE st.event_id = e.id AND st.start_time > now()
            ) nx ON TRUE
            LEFT JOIN LATERAL (
                SELECT min(s.face_value) AS min_price
                FROM seats s
                JOIN showtimes st ON st.id = s.showtime_id
                WHERE st.event_id = e.id AND st.start_time > now()
            ) mp ON TRUE
            """;

    // '' nghĩa là không lọc; bỏ dấu TRƯỚC rồi mới lower(): với locale C, lower() không đổi được chữ Đ
    private static final String FILTER = """
            WHERE (:q = '' OR lower(unaccent(e.name || ' ' || e.venue)) LIKE '%' || lower(unaccent(:q)) || '%' ESCAPE '\\')
              AND (:category = '' OR e.category = :category)
            """;

    private final JdbcClient jdbc;

    public EventQueryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<EventSummary> search(String q, String category, int page, int size) {
        String keyword = escapeLike(q == null ? "" : q.strip());
        String cat = category == null ? "" : category.strip();
        int pageSize = Math.clamp(size, 1, 50);
        int pageIndex = Math.max(page, 0);

        long total = jdbc.sql("SELECT count(*) FROM events e " + FILTER)
                .param("q", keyword).param("category", cat)
                .query(Long.class).single();
        // Sự kiện còn suất sắp diễn lên trước, sắp theo thời gian gần nhất
        List<EventSummary> content = jdbc.sql(SELECT + FILTER + """
                        ORDER BY nx.next_showtime ASC NULLS LAST, e.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .param("q", keyword).param("category", cat)
                .param("limit", pageSize).param("offset", (long) pageIndex * pageSize)
                .query((rs, rowNum) -> map(rs))
                .list();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PageResponse<>(content, pageIndex, pageSize, total, totalPages);
    }

    public EventSummary get(long id) {
        return jdbc.sql(SELECT + "WHERE e.id = :id")
                .param("id", id)
                .query((rs, rowNum) -> map(rs))
                .optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND",
                        "Không tìm thấy sự kiện có id " + id));
    }

    public List<String> categories() {
        return jdbc.sql("""
                        SELECT category FROM events
                        WHERE category IS NOT NULL AND category <> ''
                        GROUP BY category
                        ORDER BY lower(unaccent(category))
                        """)
                .query(String.class)
                .list();
    }

    private static EventSummary map(ResultSet rs) throws SQLException {
        OffsetDateTime next = rs.getObject("next_showtime", OffsetDateTime.class);
        BigDecimal minPrice = rs.getBigDecimal("min_price");
        return new EventSummary(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                rs.getString("venue"), rs.getString("image_url"), rs.getString("category"),
                next == null ? null : next.toInstant(), minPrice);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
