package com.runner.shopping.repository;

import com.runner.shopping.entity.InventoryLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLogRepository extends JpaRepository<InventoryLogs, Long> {
}
