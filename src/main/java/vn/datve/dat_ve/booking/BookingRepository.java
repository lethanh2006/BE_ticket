package vn.datve.dat_ve.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query(value = """
            SELECT DISTINCT b.id
            FROM bookings b
            JOIN booking_seats bs
                ON bs.booking_id = b.id
            WHERE bs.seat_id IN (:seatIds)
              AND bs.reservation_status = 'HELD'
              AND b.status = 'PENDING'
              AND b.expires_at <= :now
            """, nativeQuery = true)
    List<Long> findExpiredPendingBookingIdsTouchingSeats(
            @Param("seatIds") Collection<Long> seatIds,
            @Param("now") Instant now
    );
}