package vn.datve.dat_ve.booking.dto;

import java.math.BigDecimal;

import vn.datve.dat_ve.booking.BookingSeatStatus;

public record BookingSeatResponse(
        Long seatId,
        String seatCode,
        String category,
        BigDecimal unitPrice,
        BookingSeatStatus reservationStatus
) {
}
