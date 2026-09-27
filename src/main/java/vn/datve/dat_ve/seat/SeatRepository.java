package vn.datve.dat_ve.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // đọc danh sách ghế + trạng thái
    @Query(value = """
            SELECT s.id          AS "id",
                   s.seat_code   AS "seatCode",
                   s.category    AS "category",
                   s.face_value  AS "faceValue",
                   CASE
                       WHEN EXISTS (
                           SELECT 1
                           FROM booking_seats bs
                           WHERE bs.seat_id = s.id
                             AND bs.reservation_status = 'BOOKED'
                       ) THEN 'BOOKED'
                       WHEN EXISTS (
                           SELECT 1
                           FROM booking_seats bs
                           JOIN bookings b ON b.id = bs.booking_id
                           WHERE bs.seat_id = s.id
                             AND bs.reservation_status = 'HELD'
                             AND b.expires_at > now()
                       ) THEN 'HELD'
                       ELSE 'AVAILABLE'
                   END           AS "status"
            FROM seats s
            WHERE s.showtime_id = :showtimeId
            ORDER BY s.id
            """, nativeQuery = true)
    List<SeatStatusView> findSeatsWithStatus(@Param("showtimeId") Long showtimeId);


    //lấy + khóa các ghế để booking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Seat s
        WHERE s.showtimeId = :showtimeId
          AND s.id IN :seatIds
        ORDER BY s.id
        """)
    List<Seat> findSeatsForUpdate(
            @Param("showtimeId") Long showtimeId,
            @Param("seatIds") Collection<Long> seatIds
    );
}