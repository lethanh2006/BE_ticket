package vn.datve.dat_ve.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

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
}