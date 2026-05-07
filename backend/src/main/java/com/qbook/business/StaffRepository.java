package com.qbook.business;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Optional<Staff> findByTelegramId(Long telegramId);
    Optional<Staff> findByBindingCode(String bindingCode);
    List<Staff> findByBusinessId(UUID businessId);
    List<Staff> findByBusinessIdAndActiveTrue(UUID businessId);
    boolean existsByTelegramId(Long telegramId);
}
