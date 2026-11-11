-- Thông tin hiển thị của sự kiện
ALTER TABLE events
    ADD COLUMN image_url TEXT,
    ADD COLUMN category VARCHAR(50);

CREATE INDEX events_category_idx ON events (category);
CREATE INDEX showtimes_event_start_idx ON showtimes (event_id, start_time);

-- Mã vé (nội dung QR) sinh khi thanh toán thành công, và thời điểm soát vé tại cửa
ALTER TABLE bookings
    ADD COLUMN ticket_code VARCHAR(32),
    ADD COLUMN checked_in_at TIMESTAMPTZ;

CREATE UNIQUE INDEX bookings_ticket_code_key ON bookings (ticket_code) WHERE ticket_code IS NOT NULL;

-- Tìm kiếm tiếng Việt không dấu: unaccent('Lễ hội') = 'Le hoi'
CREATE EXTENSION IF NOT EXISTS unaccent;
