package vn.datve.dat_ve.booking;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false)
    private BookingSeatStatus reservationStatus;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public BookingSeat(
            Long bookingId,
            Long showtimeId,
            Long seatId,
            BookingSeatStatus reservationStatus,
            BigDecimal unitPrice
    ) {
        this.bookingId = bookingId;
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.reservationStatus = reservationStatus;
        this.unitPrice = unitPrice;
    }

    protected BookingSeat() {
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public BookingSeatStatus getReservationStatus() {
        return reservationStatus;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}