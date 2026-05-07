package com.qbook.booking;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_ref", nullable = false, unique = true)
    private String bookingRef;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "booking_type", nullable = false)
    private String bookingType;

    @Column(name = "object_id")
    private UUID objectId;

    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "start_datetime")
    private OffsetDateTime startDatetime;

    @Column(name = "end_datetime")
    private OffsetDateTime endDatetime;

    @Column(name = "scheduled_for")
    private OffsetDateTime scheduledFor;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "guests_count", nullable = false)
    @Builder.Default
    private int guestsCount = 1;

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "extras_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal extrasAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "prepayment_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal prepaymentAmount = BigDecimal.ZERO;

    @Column(name = "prepayment_status", nullable = false)
    @Builder.Default
    private String prepaymentStatus = "none";

    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private String paymentStatus = "pending";

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(name = "is_takeaway", nullable = false)
    @Builder.Default
    private boolean takeaway = false;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_zone_id")
    private UUID deliveryZoneId;

    @Column(name = "delivery_time")
    private OffsetDateTime deliveryTime;

    @Column(name = "promo_code_id")
    private UUID promoCodeId;

    @Column(name = "qr_token", nullable = false, unique = true)
    private String qrToken;

    @Column(name = "verification_code", nullable = false, length = 6)
    private String verificationCode;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(columnDefinition = "jsonb")
    private String extras;

    private String comment;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    public boolean isCompleted() { return "completed".equals(status); }
    public boolean isCancelled() { return "cancelled".equals(status); }
    public boolean isPending()   { return "pending".equals(status); }
}
