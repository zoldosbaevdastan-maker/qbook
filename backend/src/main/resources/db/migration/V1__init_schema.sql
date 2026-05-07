-- =============================================
-- QBOOK — Полная схема базы данных v1
-- =============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- БИЗНЕСЫ
-- =============================================
CREATE TABLE IF NOT EXISTS businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    telegram_id BIGINT UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('services','booking','food','shop')),
    city VARCHAR(100),
    address VARCHAR(500),
    phone VARCHAR(50),
    balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(50) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','active','suspended','blocked')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS business_profiles (
    business_id UUID PRIMARY KEY REFERENCES businesses(id) ON DELETE CASCADE,
    logo_url TEXT,
    cover_url TEXT,
    gallery_urls TEXT[] DEFAULT '{}',
    video_url TEXT,
    description TEXT,
    working_hours JSONB DEFAULT '{}',
    settings JSONB DEFAULT '{}',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- КЛИЕНТЫ
-- =============================================
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    telegram_id BIGINT UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    username VARCHAR(100),
    phone_encrypted VARCHAR(500),
    email_encrypted VARCHAR(500),
    fraud_score INT NOT NULL DEFAULT 0,
    dispute_count_month INT NOT NULL DEFAULT 0,
    is_blocked BOOLEAN NOT NULL DEFAULT false,
    block_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS client_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(255),
    ip_address INET,
    country VARCHAR(10),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- =============================================
-- SUPER ADMIN
-- =============================================
CREATE TABLE IF NOT EXISTS super_admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    telegram_id BIGINT UNIQUE,
    name VARCHAR(255) NOT NULL,
    totp_secret VARCHAR(255),
    totp_enabled BOOLEAN NOT NULL DEFAULT false,
    allowed_ips TEXT[] DEFAULT '{}',
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- СОТРУДНИКИ
-- =============================================
CREATE TABLE IF NOT EXISTS staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    telegram_id BIGINT UNIQUE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('owner','manager','staff','cashier','kitchen')),
    photo_url TEXT,
    portfolio_urls TEXT[] DEFAULT '{}',
    specialization VARCHAR(255),
    rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    reviews_count INT NOT NULL DEFAULT 0,
    binding_code VARCHAR(8),
    code_expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- УСЛУГИ
-- =============================================
CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    photo_urls TEXT[] DEFAULT '{}',
    video_url TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS staff_services (
    staff_id UUID NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (staff_id, service_id)
);

CREATE TABLE IF NOT EXISTS staff_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_working BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS schedule_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(100)
);

-- =============================================
-- ОБЪЕКТЫ БРОНИРОВАНИЯ
-- =============================================
CREATE TABLE IF NOT EXISTS bookable_objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('field','cabin','hall','court','table','room','course','tutor')),
    capacity INT,
    price_per_hour DECIMAL(10,2),
    price_per_day DECIMAL(10,2),
    photo_urls TEXT[] DEFAULT '{}',
    video_url TEXT,
    description TEXT,
    amenities JSONB DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS object_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    object_id UUID NOT NULL REFERENCES bookable_objects(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT false,
    block_reason VARCHAR(255)
);

-- =============================================
-- СТОЛИКИ
-- =============================================
CREATE TABLE IF NOT EXISTS tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    number VARCHAR(20) NOT NULL,
    zone VARCHAR(100),
    capacity INT,
    position_x INT,
    position_y INT,
    qr_token VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'free'
        CHECK (status IN ('free','occupied','reserved','cleaning')),
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS table_cart_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_id UUID NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    items JSONB NOT NULL DEFAULT '[]',
    status VARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active','ordered','closed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);

-- =============================================
-- МЕНЮ
-- =============================================
CREATE TABLE IF NOT EXISTS menu_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    category_id UUID REFERENCES menu_categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    photo_urls TEXT[] DEFAULT '{}',
    video_url TEXT,
    tags TEXT[] DEFAULT '{}',
    allergens TEXT[] DEFAULT '{}',
    calories INT,
    is_available BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- МАГАЗИН
-- =============================================
CREATE TABLE IF NOT EXISTS shop_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS shop_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    category_id UUID REFERENCES shop_categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    price_per_unit DECIMAL(10,2),
    photo_urls TEXT[] DEFAULT '{}',
    video_url TEXT,
    tags TEXT[] DEFAULT '{}',
    occasion TEXT[] DEFAULT '{}',
    flower_colors TEXT[] DEFAULT '{}',
    stock_count INT NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS delivery_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    max_distance_km DECIMAL(5,2) NOT NULL,
    delivery_price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS bouquet_compositions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(255),
    items JSONB NOT NULL DEFAULT '[]',
    total_price DECIMAL(10,2) NOT NULL,
    occasion TEXT[] DEFAULT '{}',
    flower_colors TEXT[] DEFAULT '{}',
    photo_urls TEXT[] DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- =============================================
-- КУРСЫ
-- =============================================
CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    object_id UUID REFERENCES bookable_objects(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    total_lessons INT NOT NULL,
    duration_weeks INT NOT NULL,
    schedule JSONB NOT NULL DEFAULT '[]',
    start_date DATE NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    max_students INT NOT NULL,
    enrolled_count INT NOT NULL DEFAULT 0,
    photo_urls TEXT[] DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- =============================================
-- ПРОМОКОДЫ И ЛОЯЛЬНОСТЬ
-- =============================================
CREATE TABLE IF NOT EXISTS promo_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('percent','fixed')),
    value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2),
    max_uses INT,
    used_count INT NOT NULL DEFAULT 0,
    applicable_to JSONB,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS loyalty_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    points INT NOT NULL DEFAULT 0,
    total_earned INT NOT NULL DEFAULT 0,
    total_spent INT NOT NULL DEFAULT 0,
    UNIQUE(business_id, client_id)
);

-- =============================================
-- БРОНИ / ЗАКАЗЫ
-- =============================================
CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_ref VARCHAR(50) UNIQUE NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses(id),
    client_id UUID NOT NULL REFERENCES clients(id),
    booking_type VARCHAR(50) NOT NULL CHECK (booking_type IN (
        'service','table_booking','object','food_order','shop_order','course'
    )),
    object_id UUID,
    staff_id UUID REFERENCES staff(id),
    start_datetime TIMESTAMPTZ,
    end_datetime TIMESTAMPTZ,
    scheduled_for TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,
    guests_count INT NOT NULL DEFAULT 1,
    base_amount DECIMAL(12,2) NOT NULL,
    extras_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    commission_amount DECIMAL(12,2),
    prepayment_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    prepayment_status VARCHAR(20) NOT NULL DEFAULT 'none'
        CHECK (prepayment_status IN ('none','pending','paid')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (payment_status IN ('pending','held','reserved','released','refunded','frozen')),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','confirmed','active','completed','cancelled','disputed')),
    is_takeaway BOOLEAN NOT NULL DEFAULT false,
    delivery_address TEXT,
    delivery_zone_id UUID REFERENCES delivery_zones(id),
    delivery_time TIMESTAMPTZ,
    promo_code_id UUID REFERENCES promo_codes(id),
    qr_token VARCHAR(255) UNIQUE NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    extras JSONB DEFAULT '{}',
    comment TEXT,
    cancel_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS booking_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    item_id UUID NOT NULL,
    item_type VARCHAR(50) NOT NULL CHECK (item_type IN ('menu_item','shop_item','service','course')),
    name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    comment TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','preparing','ready','served','cancelled'))
);

CREATE TABLE IF NOT EXISTS booking_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,
    actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('client','staff','owner','manager','system','admin')),
    actor_id UUID,
    details JSONB DEFAULT '{}',
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS booking_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    opened_by UUID NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    evidence_urls TEXT[] DEFAULT '{}',
    business_response TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'open'
        CHECK (status IN ('open','in_review','resolved')),
    resolution VARCHAR(30) CHECK (resolution IN ('refund_client','release_business','partial_refund')),
    resolution_comment TEXT,
    resolved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

-- =============================================
-- ФИНАНСЫ
-- =============================================
CREATE TABLE IF NOT EXISTS topup_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    payment_reference VARCHAR(100) UNIQUE NOT NULL,
    declared_amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('card','transfer','cash','mbank','other')),
    sender_card_last4 VARCHAR(4),
    paid_at TIMESTAMPTZ NOT NULL,
    screenshot_url TEXT NOT NULL,
    business_comment TEXT,
    confirmed_amount DECIMAL(12,2),
    admin_comment TEXT,
    reviewed_by UUID REFERENCES staff(id),
    reviewed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','CONFIRMED','REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS balance_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('TOPUP','COMMISSION','REFUND','ADJUSTMENT')),
    amount DECIMAL(12,2) NOT NULL,
    balance_after DECIMAL(12,2) NOT NULL,
    booking_id UUID REFERENCES bookings(id),
    topup_request_id UUID REFERENCES topup_requests(id),
    comment TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- ОТЗЫВЫ
-- =============================================
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    client_id UUID NOT NULL REFERENCES clients(id),
    staff_id UUID REFERENCES staff(id),
    booking_id UUID UNIQUE REFERENCES bookings(id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    text TEXT,
    reply TEXT,
    reply_at TIMESTAMPTZ,
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (moderation_status IN ('pending','approved','rejected')),
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- УВЕДОМЛЕНИЯ
-- =============================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_telegram_id BIGINT NOT NULL,
    business_id UUID REFERENCES businesses(id),
    business_name VARCHAR(255),
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    is_sent BOOLEAN NOT NULL DEFAULT false,
    sent_at TIMESTAMPTZ,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- АУДИТ (НЕИЗМЕНЯЕМЫЙ)
-- =============================================
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_type VARCHAR(50) NOT NULL,
    actor_id UUID,
    action VARCHAR(100) NOT NULL,
    object_type VARCHAR(100) NOT NULL,
    object_id UUID,
    old_value JSONB,
    new_value JSONB,
    ip_address INET,
    user_agent TEXT,
    session_id UUID
);

-- =============================================
-- REFRESH ТОКЕНЫ
-- =============================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    business_id UUID REFERENCES businesses(id) ON DELETE CASCADE,
    client_id UUID REFERENCES clients(id) ON DELETE CASCADE,
    staff_id UUID REFERENCES staff(id) ON DELETE CASCADE,
    jti VARCHAR(255) UNIQUE NOT NULL,
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- ВСЕ ИНДЕКСЫ
-- =============================================
CREATE INDEX IF NOT EXISTS idx_businesses_status ON businesses(status);
CREATE INDEX IF NOT EXISTS idx_businesses_type ON businesses(type);
CREATE INDEX IF NOT EXISTS idx_businesses_email ON businesses(email);

CREATE INDEX IF NOT EXISTS idx_clients_telegram ON clients(telegram_id);
CREATE INDEX IF NOT EXISTS idx_clients_fraud ON clients(fraud_score);

CREATE INDEX IF NOT EXISTS idx_staff_business ON staff(business_id);
CREATE INDEX IF NOT EXISTS idx_staff_telegram ON staff(telegram_id);
CREATE INDEX IF NOT EXISTS idx_staff_binding_code ON staff(binding_code);

CREATE INDEX IF NOT EXISTS idx_services_business ON services(business_id, is_active);
CREATE INDEX IF NOT EXISTS idx_staff_schedules_staff ON staff_schedules(staff_id);
CREATE INDEX IF NOT EXISTS idx_schedule_blocks_staff_date ON schedule_blocks(staff_id, date);

CREATE INDEX IF NOT EXISTS idx_tables_business ON tables(business_id);
CREATE INDEX IF NOT EXISTS idx_tables_qr_token ON tables(qr_token);
CREATE INDEX IF NOT EXISTS idx_cart_sessions_table ON table_cart_sessions(table_id, status);

CREATE INDEX IF NOT EXISTS idx_menu_items_business ON menu_items(business_id, is_available);
CREATE INDEX IF NOT EXISTS idx_menu_categories_business ON menu_categories(business_id);

CREATE INDEX IF NOT EXISTS idx_shop_items_business ON shop_items(business_id, is_available);
CREATE INDEX IF NOT EXISTS idx_shop_items_occasion ON shop_items USING GIN(occasion);
CREATE INDEX IF NOT EXISTS idx_shop_items_colors ON shop_items USING GIN(flower_colors);

CREATE INDEX IF NOT EXISTS idx_bookings_business ON bookings(business_id);
CREATE INDEX IF NOT EXISTS idx_bookings_client ON bookings(client_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_datetime ON bookings(start_datetime);
CREATE INDEX IF NOT EXISTS idx_bookings_type ON bookings(booking_type);
CREATE INDEX IF NOT EXISTS idx_bookings_staff ON bookings(staff_id);
CREATE INDEX IF NOT EXISTS idx_bookings_scheduled ON bookings(scheduled_for) WHERE scheduled_for IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bookings_qr_token ON bookings(qr_token);
CREATE INDEX IF NOT EXISTS idx_booking_items_booking ON booking_items(booking_id);

CREATE INDEX IF NOT EXISTS idx_topup_business_status ON topup_requests(business_id, status);
CREATE INDEX IF NOT EXISTS idx_topup_reference ON topup_requests(payment_reference);

CREATE INDEX IF NOT EXISTS idx_transactions_business ON balance_transactions(business_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON balance_transactions(type);
CREATE INDEX IF NOT EXISTS idx_transactions_created ON balance_transactions(created_at);

CREATE INDEX IF NOT EXISTS idx_reviews_business ON reviews(business_id, is_published);
CREATE INDEX IF NOT EXISTS idx_reviews_staff ON reviews(staff_id);
CREATE INDEX IF NOT EXISTS idx_reviews_booking ON reviews(booking_id);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_telegram_id, is_sent);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at);

CREATE INDEX IF NOT EXISTS idx_audit_object ON audit_log(object_type, object_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_jti ON refresh_tokens(jti);

-- =============================================
-- СУПЕРАДМИН ПО УМОЛЧАНИЮ
-- (пароль: Admin@2024! — сменить сразу!)
-- =============================================
INSERT INTO super_admins (email, password_hash, name, totp_enabled)
VALUES (
    'admin@qbook.app',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCBB10rKL.dfT6HxoZfYqTO',
    'Super Admin',
    false
) ON CONFLICT (email) DO NOTHING;
