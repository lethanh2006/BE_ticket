package vn.datve.dat_ve.seat;

/** Phát ra khi trạng thái ghế của một suất diễn thay đổi (giữ, bán, nhả ghế). */
public record SeatsChangedEvent(long showtimeId) {
}
