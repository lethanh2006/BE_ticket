package vn.datve.dat_ve.seat.dto;

import vn.datve.dat_ve.seat.SeatStatus;
import vn.datve.dat_ve.seat.SeatStatusView;

import java.math.BigDecimal;

public record SeatResponse(
        Long id,
        String seatCode,
        String category,
        BigDecimal faceValue,
        SeatStatus status
) {
    public static SeatResponse from(SeatStatusView view) {
        return new SeatResponse(
                view.getId(),
                view.getSeatCode(),
                view.getCategory(),
                view.getFaceValue(),
                SeatStatus.valueOf(view.getStatus())
        );
    }
}