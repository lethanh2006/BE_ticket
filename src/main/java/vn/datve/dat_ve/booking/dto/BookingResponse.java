package vn.datve.dat_ve.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import vn.datve.dat_ve.booking.BookingStatus;

/**
 * Đơn đặt vé kèm thông tin suất diễn và ghế.
 * refundableUntil, refundedAt, showtimeCancelledAt, showtimeCancelReason luôn null:
 * bản Java chưa hỗ trợ hoàn tiền và huỷ suất diễn. customer chỉ có trong API quản trị.
 */
public record BookingResponse(
        Long id,
        Long showtimeId,
        Long eventId,
        String eventName,
        String venue,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        List<BookingSeatResponse> seats,
        BigDecimal totalAmount,
        Instant expiresAt,
        Instant createdAt,
        String ticketCode,
        Instant checkedInAt,
        Instant refundableUntil,
        Instant refundedAt,
        Instant showtimeCancelledAt,
        String showtimeCancelReason,
        CustomerResponse customer
) {
}
