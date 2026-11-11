package vn.datve.dat_ve.booking;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository
        extends JpaRepository<BookingSeat, Long> {

    boolean existsBySeatIdInAndReservationStatusIn(
            Collection<Long> seatIds,
            Collection<BookingSeatStatus> statuses
    );

    List<BookingSeat> findByBookingIdInAndReservationStatus(
            Collection<Long> bookingIds,
            BookingSeatStatus status
    );

    List<BookingSeat> findByBookingIdAndReservationStatus(Long bookingId, BookingSeatStatus status);
}
