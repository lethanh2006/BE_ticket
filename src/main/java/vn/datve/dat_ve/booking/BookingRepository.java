package vn.datve.dat_ve.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // khoá dòng booking: thanh toán, huỷ và job hết hạn không thể chạy chồng lên nhau
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.ticketCode = :ticketCode")
    Optional<Booking> findByTicketCodeForUpdate(@Param("ticketCode") String ticketCode);
}
