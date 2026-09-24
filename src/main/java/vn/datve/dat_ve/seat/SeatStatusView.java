package vn.datve.dat_ve.seat;

import java.math.BigDecimal;

public interface SeatStatusView {

    Long getId();

    String getSeatCode();

    String getCategory();

    BigDecimal getFaceValue();

    String getStatus();
}