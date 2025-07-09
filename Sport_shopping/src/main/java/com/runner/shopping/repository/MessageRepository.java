package com.runner.shopping.repository;

import com.runner.shopping.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // Lấy tất cả message của một session, sắp xếp tăng dần theo timestamp
    List<Message> findBySessionIdOrderByTimestampAsc(Long sessionId);


}
