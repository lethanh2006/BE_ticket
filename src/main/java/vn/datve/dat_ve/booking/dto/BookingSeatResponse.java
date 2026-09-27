package vn.datve.dat_ve.booking.dto;

import java.math.BigDecimal;

public record BookingSeatResponse(
        Long seatId,
        String seatCode,
        BigDecimal unitPrice
) {
}