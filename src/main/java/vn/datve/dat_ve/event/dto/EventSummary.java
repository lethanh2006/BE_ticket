package vn.datve.dat_ve.event.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Sự kiện kèm suất diễn sắp tới gần nhất và giá vé thấp nhất (để hiển thị danh sách). */
public record EventSummary(
        Long id,
        String name,
        String description,
        String venue,
        String imageUrl,
        String category,
        Instant nextShowtime,
        BigDecimal minPrice
) {
}
