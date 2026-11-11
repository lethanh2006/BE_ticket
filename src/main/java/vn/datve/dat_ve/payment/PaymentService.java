package vn.datve.dat_ve.payment;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.datve.dat_ve.booking.Booking;
import vn.datve.dat_ve.booking.BookingRepository;
import vn.datve.dat_ve.booking.BookingSeat;
import vn.datve.dat_ve.booking.BookingSeatRepository;
import vn.datve.dat_ve.booking.BookingSeatStatus;
import vn.datve.dat_ve.booking.BookingStatus;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.payment.dto.PaymentResponse;
import vn.datve.dat_ve.seat.SeatsChangedEvent;

/**
 * Thanh toán giả lập, idempotent theo header Idempotency-Key:
 * gửi lại cùng key (do mạng chập chờn, bấm hai lần) nhận lại đúng kết quả cũ, không trừ tiền hai lần.
 */
@Service
public class PaymentService {

    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_-]{8,100}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher events;

    public PaymentService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository, ApplicationEventPublisher events) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.events = events;
    }

    public record Result(PaymentResponse payment, boolean replayed) {
    }

    @Transactional
    public Result pay(long userId, long bookingId, String idempotencyKey, String simulate) {
        if (idempotencyKey == null || !VALID_KEY.matcher(idempotencyKey).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                    "Thiếu hoặc sai header Idempotency-Key (8-100 ký tự chữ, số, '-' hoặc '_')");
        }

        // 1. Khoá đơn trước: hai request thanh toán cùng đơn sẽ xếp hàng ở đây,
        //    request sau thấy ngay kết quả của request trước.
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .filter(b -> b.isOwnedBy(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Không tìm thấy đơn đặt vé"));

        // 2. Key đã dùng: trả lại kết quả cũ (cùng đơn) hoặc từ chối (key của đơn khác)
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getBookingId().equals(bookingId)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key đã được dùng cho một đơn khác");
            }
            return new Result(PaymentResponse.from(existing.get(), booking.getStatus()), true);
        }

        // 3. Chỉ thanh toán được đơn đang giữ ghế và chưa hết hạn
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_PAYABLE",
                    "Đơn đã được thanh toán, đã huỷ hoặc đã hết hạn");
        }
        if (booking.isHoldExpired(Instant.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_EXPIRED",
                    "Đã hết thời gian giữ ghế, vui lòng đặt lại");
        }

        // 4. Gọi "cổng thanh toán" giả lập
        boolean success = !"FAILED".equals(simulate);
        Payment payment = paymentRepository.save(new Payment(bookingId, idempotencyKey, "MOCK",
                "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED, booking.getTotalAmount()));

        // 5. Thành công: xác nhận đơn, chuyển ghế từ giữ chỗ sang đã bán, sinh mã vé
        if (success) {
            booking.confirm(newTicketCode());
            bookingSeatRepository.findByBookingIdAndReservationStatus(bookingId, BookingSeatStatus.HELD)
                    .forEach(BookingSeat::book);
            events.publishEvent(new SeatsChangedEvent(booking.getShowtimeId()));
        }
        return new Result(PaymentResponse.from(payment, booking.getStatus()), false);
    }

    /** 16 ký tự hex ngẫu nhiên (64 bit), dùng làm nội dung mã QR trên vé. */
    private static String newTicketCode() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
