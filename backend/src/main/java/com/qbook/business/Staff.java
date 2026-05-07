package com.qbook.business;

import com.qbook.common.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "staff")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "portfolio_urls", columnDefinition = "TEXT[]")
    private String[] portfolioUrls;

    private String specialization;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "reviews_count", nullable = false)
    @Builder.Default
    private int reviewsCount = 0;

    @Column(name = "binding_code", length = 8)
    private String bindingCode;

    @Column(name = "code_expires_at")
    private OffsetDateTime codeExpiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public boolean isBindingCodeValid() {
        return bindingCode != null && codeExpiresAt != null
                && codeExpiresAt.isAfter(OffsetDateTime.now());
    }
}
