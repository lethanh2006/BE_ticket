package vn.datve.dat_ve.admin.dto;

import vn.datve.dat_ve.booking.dto.BookingResponse;

/** result: CHECKED_IN (vào cửa lần đầu) hoặc ALREADY_CHECKED_IN (vé đã được quét trước đó). */
public record CheckInResponse(String result, BookingResponse booking) {
}
