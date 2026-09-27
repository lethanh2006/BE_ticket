package vn.datve.dat_ve.booking.dto;

import vn.datve.dat_ve.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id,
        Long showtimeId,
        BookingStatus status,
        List<BookingSeatResponse> seats,
        BigDecimal totalAmount,
        Instant expiresAt
) {
}