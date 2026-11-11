package vn.datve.dat_ve.showtime.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** cancelledAt/cancelReason luôn null: bản Java chưa hỗ trợ huỷ suất diễn. */
public record ShowtimeResponse(
        Long id,
        Long eventId,
        Instant startTime,
        Instant endTime,
        long totalSeats,
        long availableSeats,
        BigDecimal minPrice,
        Instant cancelledAt,
        String cancelReason
) {
}
