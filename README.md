# ĐặtVé API: backend đặt vé sự kiện (Java / Spring Boot)

REST API cho website đặt vé sự kiện: xem sự kiện và suất diễn, chọn ghế trên sơ đồ, giữ ghế, thanh
toán, nhận vé QR và soát vé tại cửa. Trọng tâm là đúng dữ liệu khi nhiều người tranh cùng một ghế và
khi client gửi lại request thanh toán.

- **Giao diện:** https://datve-lethanh.vercel.app (React, repo [fe_datve](https://github.com/lethanh2006/fe_datve))
- **Swagger UI:** https://datve-lethanh-api.onrender.com/swagger-ui.html
- **Tài khoản dùng thử:** `user@datve.local` / `User@12345`
- Máy chủ chạy gói miễn phí nên tự ngủ khi không có truy cập; lần đầu mở có thể mất khoảng 1 phút.

**Công nghệ:** Java 21, Spring Boot 4.1 (Spring MVC, Spring Data JPA/Hibernate, Spring Security +
JWT), PostgreSQL 17, Flyway, JUnit + Testcontainers, Docker, Render, Neon.

## Điểm đáng chú ý

| Bài toán | Cách giải |
| --- | --- |
| Hai người cùng đặt một ghế | Khoá các ghế bằng `SELECT … FOR UPDATE` (theo thứ tự id) trong lúc tạo đơn; partial unique index trên `booking_seats` chỉ cho mỗi ghế một lượt giữ/bán còn hiệu lực. Test: 10 người tranh một ghế, đúng 1 người được |
| Ghế giữ mãi không thanh toán | Đơn giữ ghế 5 phút; job `@Scheduled` mỗi phút chuyển đơn quá hạn sang EXPIRED và nhả ghế trong một câu SQL (CTE `UPDATE … RETURNING`) |
| Gửi lại request thanh toán | Header `Idempotency-Key` (unique trong DB). Khoá dòng đơn trước rồi mới tra key, nên request trùng chờ nhau và nhận lại đúng kết quả cũ (`Idempotent-Replayed: true`) |
| Suất diễn trùng giờ | Ràng buộc `EXCLUDE USING gist` (btree_gist + `tstzrange`) ngay trong PostgreSQL |
| Sơ đồ ghế cập nhật tức thì | Server-Sent Events (`SseEmitter`), gửi sau khi transaction commit; trình duyệt tự làm mới định kỳ nếu mất kết nối |
| Tìm kiếm tiếng Việt | `lower(unaccent(...))`: gõ "le hoi" vẫn tìm ra "Lễ hội" |
| Bảo mật | Spring Security + JWT HS256, mật khẩu BCrypt, phân quyền USER/ADMIN; người dùng lấy từ token chứ không nhận từ client; đơn của người khác trả 404 |
| Báo cáo quản trị | Doanh thu theo ngày (`generate_series` để ngày trống vẫn hiện 0), top sự kiện, số đơn theo trạng thái bằng SQL tổng hợp |

Phía đọc (danh sách sự kiện, đơn đặt vé, thống kê) viết bằng SQL qua `JdbcClient` để lấy đủ dữ liệu
trong 1–2 câu truy vấn, tránh N+1; phía ghi dùng JPA. Lỗi luôn trả về `{code, message, timestamp}`.

## API chính

| Phương thức | Đường dẫn | Mô tả |
| --- | --- | --- |
| POST | `/api/auth/register`, `/api/auth/login` | Đăng ký, đăng nhập (trả JWT) |
| GET/PUT | `/api/auth/me`, PUT `/api/auth/password` | Hồ sơ, đổi mật khẩu |
| GET | `/api/events?q=&category=&page=&size=` | Danh sách sự kiện, tìm không dấu |
| GET | `/api/events/{id}/showtimes`, `/api/showtimes/{id}/seats` | Suất diễn, sơ đồ ghế |
| GET | `/api/showtimes/{id}/seats/events` | Luồng SSE báo ghế thay đổi |
| POST | `/api/showtimes/{id}/bookings` | Giữ ghế (cần đăng nhập) |
| GET/POST | `/api/bookings`, `/api/bookings/{id}`, `/api/bookings/{id}/cancel` | Đơn của tôi, huỷ đơn |
| POST | `/api/bookings/{id}/payments` (header `Idempotency-Key`) | Thanh toán giả lập |
| GET/POST | `/api/admin/stats`, `/api/admin/bookings`, `/api/admin/bookings/check-in` | Thống kê, quản lý đơn, soát vé (ADMIN) |
| POST/PUT/DELETE | `/api/admin/events`, `/api/admin/events/{id}/showtimes`, `/api/admin/showtimes/{id}/seats` | Quản lý danh mục (ADMIN) |

## Chạy local

Cần JDK 21 và PostgreSQL (hoặc Docker: `docker compose up -d`).

```bash
cp .env.example .env      # điền DB_PASSWORD; JWT_SECRET có giá trị mặc định cho dev
./mvnw spring-boot:run    # http://localhost:8080/swagger-ui.html, profile dev tạo sẵn dữ liệu mẫu
```

## Kiểm thử

```bash
./mvnw test   # test tích hợp qua HTTP trên PostgreSQL thật (Testcontainers, cần Docker)
```
