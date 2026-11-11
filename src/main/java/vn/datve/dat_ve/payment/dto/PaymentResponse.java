package vn.datve.dat_ve.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import vn.datve.dat_ve.booking.BookingStatus;
import vn.datve.dat_ve.payment.Payment;
import vn.datve.dat_ve.payment.PaymentStatus;

public record PaymentResponse(
        Long id,
        Long bookingId,
        PaymentStatus status,
        BigDecimal amount,
        String provider,
        String providerReference,
        Instant createdAt,
        Instant updatedAt,
        BookingStatus bookingStatus
) {

    public static PaymentResponse from(Payment payment, BookingStatus bookingStatus) {
        return new PaymentResponse(payment.getId(), payment.getBookingId(), payment.getStatus(), payment.getAmount(),
                payment.getProvider(), payment.getProviderReference(), payment.getCreatedAt(), payment.getUpdatedAt(),
                bookingStatus);
    }
}
