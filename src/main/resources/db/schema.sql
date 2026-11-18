-- =============================================================================
-- schema.sql - Cơ sở dữ liệu repair-workshop-queue (PostgreSQL)
-- Khớp với docs/contract.md (mục 2, 4, 5). Chạy lại nhiều lần được:
-- script xóa bảng cũ rồi tạo lại, nên chỉ chạy trên DB dev/test, không chạy
-- trên DB đang có dữ liệu cần giữ.
--
-- Bản này giữ nguyên 7 bảng cũ và BỔ SUNG:
--   - warranties : tab Warranty (tạo bảo hành + import bảo hành legacy)
--   - payments   : luật "chặn đóng job khi chưa thanh toán"
--   - trigger tự cập nhật updated_at
--   - index cho các ô lọc / sắp xếp trên UI
--   - view phục vụ bảng ở tab Repair Queue và Warranty
-- =============================================================================

-- 1. Xóa đối tượng cũ (view trước, rồi bảng con trước, bảng cha sau).
DROP VIEW  IF EXISTS v_warranty_candidates CASCADE;
DROP VIEW  IF EXISTS v_warranty_list       CASCADE;
DROP VIEW  IF EXISTS v_repair_queue        CASCADE;

DROP TABLE IF EXISTS payments        CASCADE;
DROP TABLE IF EXISTS warranties      CASCADE;
DROP TABLE IF EXISTS status_history  CASCADE;
DROP TABLE IF EXISTS labor_services  CASCADE;
DROP TABLE IF EXISTS parts           CASCADE;
DROP TABLE IF EXISTS estimates       CASCADE;
DROP TABLE IF EXISTS repair_jobs     CASCADE;
DROP TABLE IF EXISTS devices         CASCADE;
DROP TABLE IF EXISTS customers       CASCADE;

DROP FUNCTION IF EXISTS set_updated_at() CASCADE;

-- 2. customers (contract 4.1)
CREATE TABLE customers (
    id          SERIAL        PRIMARY KEY,
    full_name   VARCHAR(100)  NOT NULL,
    phone       VARCHAR(20)   NOT NULL,
    CONSTRAINT chk_customers_phone_length CHECK (phone ~ '^[0-9]{10}$'),
    email       VARCHAR(100),
    address     VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. devices (contract 4.2)
CREATE TABLE devices (
    id             SERIAL       PRIMARY KEY,
    customer_id    INT          NOT NULL
                   REFERENCES customers (id) ON DELETE RESTRICT,
    device_type    VARCHAR(50)  NOT NULL,
    brand          VARCHAR(50),
    model          VARCHAR(50),
    serial_number  VARCHAR(50),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. repair_jobs (contract 4.3)
--    - desired_days: số ngày khách mong muốn nhận hàng, tính từ created_at.
--      Hạn chót = created_at + desired_days (không lưu, tính khi truy vấn).
--    - estimated_ready_at: ước tính do nhân viên nhập tay, có thể NULL.
--    - chk_closed_at: closed_at có giá trị khi và chỉ khi status = 'CLOSED',
--      nên khi đóng đơn phải UPDATE status và closed_at trong CÙNG một câu lệnh.
--    - Cột "Received At" trên tab Repair Queue chính là created_at.
CREATE TABLE repair_jobs (
    id                  SERIAL       PRIMARY KEY,
    device_id           INT          NOT NULL
                        REFERENCES devices (id) ON DELETE RESTRICT,
    issue_description   TEXT         NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'RECEIVED',
    desired_days        INT          NOT NULL,
    estimated_ready_at  TIMESTAMP,
    closed_at           TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_repair_jobs_status CHECK (status IN
        ('RECEIVED', 'DIAGNOSING', 'WAITING_FOR_APPROVAL',
         'REPAIRING', 'READY', 'CLOSED')),
    CONSTRAINT chk_repair_jobs_desired_days CHECK (desired_days > 0),
    CONSTRAINT chk_repair_jobs_closed_at CHECK
        ((status = 'CLOSED') = (closed_at IS NOT NULL))
);

-- 5. estimates (contract 4.4): quan hệ 1-N với repair_jobs, có version.
--    Tổng tiền không lưu ở đây, tính trong Java từ parts và labor_services.
CREATE TABLE estimates (
    id          SERIAL       PRIMARY KEY,
    job_id      INT          NOT NULL
                REFERENCES repair_jobs (id) ON DELETE RESTRICT,
    version     INT          NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    note        TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_estimates_status  CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_estimates_version CHECK (version > 0),
    CONSTRAINT uq_estimates_job_version UNIQUE (job_id, version)
);

-- Mỗi job chỉ có tối đa MỘT estimate đã duyệt (phục vụ luật chặn đóng job).
CREATE UNIQUE INDEX uq_estimates_one_approved_per_job
    ON estimates (job_id) WHERE status = 'APPROVED';

-- 6. parts (contract 4.5): dòng linh kiện của một estimate
CREATE TABLE parts (
    id           SERIAL         PRIMARY KEY,
    estimate_id  INT            NOT NULL
                 REFERENCES estimates (id) ON DELETE RESTRICT,
    name         VARCHAR(100)   NOT NULL,
    unit_price   NUMERIC(12,2)  NOT NULL,
    quantity     INT            NOT NULL,

    CONSTRAINT chk_parts_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_parts_quantity   CHECK (quantity > 0)
);

-- 7. labor_services (contract 4.6): dòng công thợ của một estimate
CREATE TABLE labor_services (
    id           SERIAL         PRIMARY KEY,
    estimate_id  INT            NOT NULL
                 REFERENCES estimates (id) ON DELETE RESTRICT,
    description  VARCHAR(200)   NOT NULL,
    hours        NUMERIC(6,2)   NOT NULL,
    hourly_rate  NUMERIC(12,2)  NOT NULL,

    CONSTRAINT chk_labor_hours       CHECK (hours > 0),
    CONSTRAINT chk_labor_hourly_rate CHECK (hourly_rate >= 0)
);

-- 8. status_history (contract 4.7)
--    old_status là NULL ở dòng đầu tiên (lúc tạo job).
--    Tab Job History đọc thẳng từ bảng này (History ID, Repair ID,
--    Previous Status, New Status, Note, Status Saved At = changed_at).
CREATE TABLE status_history (
    id          SERIAL       PRIMARY KEY,
    job_id      INT          NOT NULL
                REFERENCES repair_jobs (id) ON DELETE RESTRICT,
    old_status  VARCHAR(30),
    new_status  VARCHAR(30)  NOT NULL,
    note        TEXT,
    changed_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_history_old_status CHECK (old_status IS NULL OR old_status IN
        ('RECEIVED', 'DIAGNOSING', 'WAITING_FOR_APPROVAL',
         'REPAIRING', 'READY', 'CLOSED')),
    CONSTRAINT chk_history_new_status CHECK (new_status IN
        ('RECEIVED', 'DIAGNOSING', 'WAITING_FOR_APPROVAL',
         'REPAIRING', 'READY', 'CLOSED'))
);

-- =============================================================================
-- PHẦN BỔ SUNG
-- =============================================================================

-- 9. payments: thanh toán cho một estimate (thường là estimate đã APPROVED).
--    Luật "chặn đóng job khi chưa thanh toán / chưa duyệt" kiểm tra trong Java:
--      (a) job có estimate APPROVED
--      (b) SUM(payments.amount) của estimate đó >= tổng estimate (tính từ
--          parts + labor_services)
--    Cho phép nhiều dòng payment (trả góp / đặt cọc), nên không UNIQUE.
CREATE TABLE payments (
    id           SERIAL         PRIMARY KEY,
    estimate_id  INT            NOT NULL
                 REFERENCES estimates (id) ON DELETE RESTRICT,
    amount       NUMERIC(12,2)  NOT NULL,
    method       VARCHAR(20)    NOT NULL DEFAULT 'CASH',
    paid_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note         TEXT,

    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_method CHECK (method IN
        ('CASH', 'CARD', 'BANK_TRANSFER', 'OTHER'))
);

-- 10. warranties: tab Warranty (Warranty ID, Repair ID, Device, Customer,
--     Start Date, End Date).
--     - source = 'NATIVE': tạo từ nút "Create Warranty" cho một repair job,
--       bắt buộc có repair_job_id.
--     - source = 'LEGACY': import từ định dạng cũ qua Adapter. Bản ghi cũ có
--       thể không có repair job trong hệ thống mới, nên repair_job_id được
--       phép NULL; khi đó device_id vẫn xác định được thiết bị và khách hàng.
--     - legacy_ref: mã bản ghi trong hệ thống cũ, UNIQUE để import lại
--       cùng một file không tạo trùng.
--     - Trạng thái còn hạn / hết hạn KHÔNG lưu: tính từ end_date so với ngày
--       hiện tại (xem v_warranty_list), dùng cho Total / Valid / Expired.
CREATE TABLE warranties (
    id             SERIAL        PRIMARY KEY,
    repair_job_id  INT           REFERENCES repair_jobs (id) ON DELETE RESTRICT,
    device_id      INT           NOT NULL
                   REFERENCES devices (id) ON DELETE RESTRICT,
    start_date     DATE          NOT NULL,
    end_date       DATE          NOT NULL,
    source         VARCHAR(10)   NOT NULL DEFAULT 'NATIVE',
    legacy_ref     VARCHAR(100),
    note           TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_warranties_dates  CHECK (end_date >= start_date),
    CONSTRAINT chk_warranties_source CHECK (source IN ('NATIVE', 'LEGACY')),
    CONSTRAINT chk_warranties_origin CHECK
        (source = 'LEGACY' OR repair_job_id IS NOT NULL),
    CONSTRAINT chk_warranties_legacy_ref CHECK
        (source = 'LEGACY' OR legacy_ref IS NULL)
);

-- Mỗi repair job có tối đa một bảo hành (NULL của legacy không bị tính trùng).
CREATE UNIQUE INDEX uq_warranties_repair_job
    ON warranties (repair_job_id) WHERE repair_job_id IS NOT NULL;
CREATE UNIQUE INDEX uq_warranties_legacy_ref
    ON warranties (legacy_ref) WHERE legacy_ref IS NOT NULL;

-- 11. Trigger tự cập nhật updated_at khi UPDATE (khỏi phải nhớ set trong Java).
CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customers_updated_at   BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_devices_updated_at     BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_repair_jobs_updated_at BEFORE UPDATE ON repair_jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_estimates_updated_at   BEFORE UPDATE ON estimates
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_warranties_updated_at  BEFORE UPDATE ON warranties
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 12. Index (contract mục 5 + phần bổ sung).
--     PostgreSQL không tự tạo index cho cột khóa ngoại, nên tạo cho các cột
--     hay dùng trong JOIN / WHERE.
--     estimates(job_id) đã được phủ bởi UNIQUE (job_id, version) ở trên.
CREATE INDEX idx_devices_customer_id        ON devices (customer_id);
CREATE INDEX idx_repair_jobs_status         ON repair_jobs (status);
CREATE INDEX idx_repair_jobs_device_id      ON repair_jobs (device_id);
CREATE INDEX idx_parts_estimate_id          ON parts (estimate_id);
CREATE INDEX idx_labor_services_estimate_id ON labor_services (estimate_id);
CREATE INDEX idx_status_history_job_id      ON status_history (job_id);

-- Bổ sung cho các ô lọc / sắp xếp trên UI:
CREATE INDEX idx_repair_jobs_created_at     ON repair_jobs (created_at);   -- Newest/Oldest
CREATE INDEX idx_status_history_changed_at  ON status_history (changed_at);-- From/To date
CREATE INDEX idx_customers_phone            ON customers (phone);
CREATE INDEX idx_payments_estimate_id       ON payments (estimate_id);
CREATE INDEX idx_warranties_device_id       ON warranties (device_id);
CREATE INDEX idx_warranties_end_date        ON warranties (end_date);

-- 13. View cho UI (chỉ đọc, tiện cho JDBC: SELECT * FROM v_... WHERE ...)

-- 13.1 Tab Repair Queue: ID, Device Name, Symptoms, Status, Received At,
--      Customer Name, Customer Phone (+ hạn chót khách mong muốn).
CREATE VIEW v_repair_queue AS
SELECT j.id                                   AS id,
       COALESCE(NULLIF(TRIM(CONCAT_WS(' ', d.brand, d.model)), ''),
                d.device_type)                AS device_name,
       j.issue_description                    AS symptoms,
       j.status                               AS status,
       j.created_at                           AS received_at,
       c.full_name                            AS customer_name,
       c.phone                                AS customer_phone,
       j.created_at + j.desired_days * INTERVAL '1 day' AS deadline_at,
       j.estimated_ready_at                   AS estimated_ready_at
FROM repair_jobs j
JOIN devices   d ON d.id = j.device_id
JOIN customers c ON c.id = d.customer_id;

-- 13.2 Tab Warranty (danh sách + dashboard Total / Valid / Expired).
CREATE VIEW v_warranty_list AS
SELECT w.id                                   AS warranty_id,
       w.repair_job_id                        AS repair_id,
       COALESCE(NULLIF(TRIM(CONCAT_WS(' ', d.brand, d.model)), ''),
                d.device_type)                AS device_name,
       c.full_name                            AS customer_name,
       w.start_date                           AS start_date,
       w.end_date                             AS end_date,
       w.source                               AS source,
       CASE WHEN w.end_date >= CURRENT_DATE
            THEN 'VALID' ELSE 'EXPIRED' END   AS warranty_status
FROM warranties w
JOIN devices   d ON d.id = w.device_id
JOIN customers c ON c.id = d.customer_id;

-- 13.3 Bảng chọn job để tạo bảo hành: job đã sửa xong (READY/CLOSED) và chưa
--      có bảo hành. "Completed At" lấy từ lần chuyển sang READY gần nhất.
CREATE VIEW v_warranty_candidates AS
SELECT j.id                                   AS repair_id,
       d.id                                   AS device_id,
       COALESCE(NULLIF(TRIM(CONCAT_WS(' ', d.brand, d.model)), ''),
                d.device_type)                AS device_name,
       c.full_name                            AS customer_name,
       j.created_at                           AS received_at,
       (SELECT MAX(h.changed_at)
          FROM status_history h
         WHERE h.job_id = j.id AND h.new_status = 'READY') AS completed_at
FROM repair_jobs j
JOIN devices   d ON d.id = j.device_id
JOIN customers c ON c.id = d.customer_id
WHERE j.status IN ('READY', 'CLOSED')
  AND NOT EXISTS (SELECT 1 FROM warranties w WHERE w.repair_job_id = j.id);