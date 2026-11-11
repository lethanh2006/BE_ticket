package vn.datve.dat_ve.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import vn.datve.dat_ve.booking.BookingStatus;

public record StatsResponse(
        long events,
        long upcomingShowtimes,
        long users,
        Map<BookingStatus, Long> bookingsByStatus,
        long seatsSold,
        BigDecimal revenue,
        List<Daily> daily,
        List<TopEvent> topEvents
) {

    public record Daily(LocalDate date, long confirmedBookings, BigDecimal revenue) {
    }

    public record TopEvent(long eventId, String name, long seatsSold, BigDecimal revenue) {
    }
}
