package vn.datve.dat_ve.payment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.auth.CurrentUser;
import vn.datve.dat_ve.payment.dto.PaymentRequest;
import vn.datve.dat_ve.payment.dto.PaymentResponse;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 201 cho lần thanh toán mới, 200 khi trả lại kết quả cũ của cùng Idempotency-Key
    @PostMapping("/api/bookings/{bookingId}/payments")
    public ResponseEntity<PaymentResponse> pay(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody(required = false) PaymentRequest request) {
        PaymentService.Result result = paymentService.pay(CurrentUser.id(jwt), bookingId, idempotencyKey,
                request == null ? null : request.simulate());
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(result.payment());
    }
}
