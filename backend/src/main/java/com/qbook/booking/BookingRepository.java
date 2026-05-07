package com.qbook.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingRef(String bookingRef);
    Optional<Booking> findByQrToken(String qrToken);
    Optional<Booking> findByIdempotencyKey(String key);

    List<Booking> findByBusinessIdAndStatus(UUID businessId, String status);
    List<Booking> findByClientId(UUID clientId);
    List<Booking> findByStaffIdAndStartDatetimeBetween(
        UUID staffId, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT b FROM Booking b WHERE b.businessId = :businessId " +
           "AND b.status NOT IN ('cancelled','completed') " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findActiveByBusiness(UUID businessId);

    @Query("SELECT b FROM Booking b WHERE b.scheduledFor IS NOT NULL " +
           "AND b.scheduledFor BETWEEN :from AND :to " +
           "AND b.status = 'confirmed'")
    List<Booking> findScheduledBetween(OffsetDateTime from, OffsetDateTime to);

    long countByBusinessIdAndCreatedAtAfter(UUID businessId, OffsetDateTime after);
}
