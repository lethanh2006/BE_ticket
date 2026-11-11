package vn.datve.dat_ve.payment.dto;

import jakarta.validation.constraints.Pattern;

/** simulate: kết quả mong muốn của cổng thanh toán giả lập (mặc định SUCCESS). */
public record PaymentRequest(
        @Pattern(regexp = "SUCCESS|FAILED", message = "simulate chỉ nhận SUCCESS hoặc FAILED")
        String simulate
) {
}
