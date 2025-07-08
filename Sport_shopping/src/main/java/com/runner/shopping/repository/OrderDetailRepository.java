package com.runner.shopping.repository;

import com.runner.shopping.entity.OrderDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetails, Long> {

    List<OrderDetails> findByOrderId(Long orderId);

    //Top 10 sản phẩm bán chạy
    //o.status = 'DELIVERED' AND
    @Query(value = """
        SELECT p.id, p.name, pv.id, pv.sku, SUM(od.quantity)
        FROM order_details od
        JOIN product_variants pv ON od.variant_id = pv.id
        JOIN products p ON pv.product_id = p.id
        JOIN orders o ON od.order_id = o.id
        WHERE o.status = 'DELIVERED' AND o.payment_status = 'COMPLETED'
          AND (:startDate IS NULL OR o.created_at >= :startDate)
          AND (:endDate IS NULL OR o.created_at <= :endDate)
        GROUP BY p.id, p.name, pv.id, pv.sku
        ORDER BY SUM(od.quantity) DESC, p.created_at ASC, p.name ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopSellingProducts(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("limit") int limit);
}
