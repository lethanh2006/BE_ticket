package vn.datve.dat_ve.booking;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "ticket_code")
    private String ticketCode;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "updated_at", insertable = false)
    private Instant updatedAt;

    public Booking(
            Long userId,
            Long showtimeId,
            BookingStatus status,
            BigDecimal totalAmount,
            Instant expiresAt
    ) {
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.expiresAt = expiresAt;
    }

    protected Booking() {
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getShowtimeId() { return showtimeId; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getTicketCode() { return ticketCode; }
    public Instant getCheckedInAt() { return checkedInAt; }

    public boolean isOwnedBy(long candidateUserId) {
        return userId == candidateUserId;
    }

    /** Đơn chờ thanh toán đã quá hạn giữ ghế (dù job dọn dẹp chưa kịp đổi trạng thái). */
    public boolean isHoldExpired(Instant now) {
        return status == BookingStatus.PENDING && expiresAt != null && !expiresAt.isAfter(now);
    }

    public void expire() {
        this.status = BookingStatus.EXPIRED;
        touch();
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
        touch();
    }

    /** Thanh toán thành công: đơn được xác nhận và có mã vé để soát vé tại cửa. */
    public void confirm(String ticketCode) {
        this.status = BookingStatus.CONFIRMED;
        this.ticketCode = ticketCode;
        touch();
    }

    public void checkIn(Instant now) {
        this.checkedInAt = now;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
