package vn.datve.dat_ve.seat;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.datve.dat_ve.common.exception.ApiException;

/**
 * Đẩy thông báo "ghế đã đổi" tới trình duyệt qua Server-Sent Events.
 * Chỉ gửi sau khi transaction commit, để trình duyệt tải lại thì thấy dữ liệu mới.
 * Danh sách kết nối nằm trong bộ nhớ: đủ cho một instance; chạy nhiều instance thì cần
 * kênh chung như PostgreSQL LISTEN/NOTIFY hoặc Redis pub/sub.
 */
@Service
public class SeatStreamService {

    private static final long TIMEOUT_MS = Duration.ofMinutes(10).toMillis();
    private static final int MAX_CONNECTIONS = 1000;

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final AtomicInteger connections = new AtomicInteger();

    public SseEmitter subscribe(long showtimeId) {
        if (connections.incrementAndGet() > MAX_CONNECTIONS) {
            connections.decrementAndGet();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TOO_MANY_STREAMS",
                    "Máy chủ đang quá tải, giao diện sẽ tự làm mới định kỳ");
        }
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Set<SseEmitter> showtimeEmitters = emitters.computeIfAbsent(showtimeId, id -> new CopyOnWriteArraySet<>());
        showtimeEmitters.add(emitter);
        Runnable remove = () -> {
            if (showtimeEmitters.remove(emitter)) {
                connections.decrementAndGet();
            }
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        // gửi ngay một dòng chú thích để trình duyệt nhận header và mở kết nối
        send(emitter, SseEmitter.event().comment("connected"), remove);
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSeatsChanged(SeatsChangedEvent event) {
        Set<SseEmitter> showtimeEmitters = emitters.get(event.showtimeId());
        if (showtimeEmitters == null) {
            return;
        }
        for (SseEmitter emitter : showtimeEmitters) {
            send(emitter, SseEmitter.event().name("seats").data(event.showtimeId()),
                    () -> showtimeEmitters.remove(emitter));
        }
    }

    /** Proxy thường cắt kết nối im lặng quá lâu; gửi nhịp tim mỗi 25 giây. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.values().forEach(set -> set.forEach(emitter ->
                send(emitter, SseEmitter.event().comment("ping"), () -> set.remove(emitter))));
    }

    private static void send(SseEmitter emitter, SseEmitter.SseEventBuilder event, Runnable onFailure) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            onFailure.run();
            emitter.completeWithError(exception);
        }
    }
}
