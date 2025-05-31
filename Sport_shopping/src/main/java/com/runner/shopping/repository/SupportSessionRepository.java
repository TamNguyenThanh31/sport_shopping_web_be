package com.runner.shopping.repository;

import com.runner.shopping.entity.SupportSession;
import com.runner.shopping.entity.User;
import com.runner.shopping.enums.SupportSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportSessionRepository extends JpaRepository<SupportSession, Long> {
    // Tìm session đang active (ended_at IS NULL) của customer
    Optional<SupportSession> findByCustomerIdAndEndedAtIsNull(Long customerId);

    // Tìm tất cả session chờ (chưa assign staff, tức staffId IS NULL, và còn active)
    List<SupportSession> findByStaffIdIsNullAndEndedAtIsNullOrderByLastActivityDesc();

    // Tìm tất cả session mà staff đã được assign (đã assign và còn active), để nhân viên có thể xem
    List<SupportSession> findByStaffIdAndEndedAtIsNullOrderByLastActivityDesc(Long staffId);

    // Lấy danh sách session active của một user (có thể là customer hoặc staff)
    List<SupportSession> findByCustomerIdOrStaffIdAndEndedAtIsNullOrderByLastActivityDesc(Long customerId, Long staffId);
}
