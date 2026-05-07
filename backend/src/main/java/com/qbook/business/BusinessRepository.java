package com.qbook.business;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByEmail(String email);
    Optional<Business> findByTelegramId(Long telegramId);
    boolean existsByEmail(String email);
    List<Business> findByStatus(Business.BusinessStatus status);
    List<Business> findByType(Business.BusinessType type);

    @Query("SELECT b FROM Business b WHERE b.balance < :threshold AND b.status = 'active'")
    List<Business> findByBalanceLessThan(BigDecimal threshold);

    @Modifying
    @Query("UPDATE Business b SET b.balance = b.balance + :amount WHERE b.id = :id")
    int addBalance(UUID id, BigDecimal amount);

    @Modifying
    @Query("UPDATE Business b SET b.balance = b.balance - :amount WHERE b.id = :id AND b.balance >= :amount")
    int deductBalance(UUID id, BigDecimal amount);
}
