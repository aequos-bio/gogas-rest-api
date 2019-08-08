package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SupplierOrderItemRepo extends CrudRepository<SupplierOrderItem, String> {

    List<SupplierOrderItem> findByOrderId(String orderId);

    Optional<SupplierOrderItem> findByOrderIdAndProductId(String orderId, String productId);

    @Transactional
    @Modifying
    @Query("UPDATE SupplierOrderItem s SET s.boxesCount = ?3, s.totalQuantity = ?3 * s.boxWeight WHERE s.orderId = ?1 AND productId = ?2")
    int updateBoxesByOrderIdAndProductId(String orderId, String productId, BigDecimal boxes);

    @Modifying
    @Query("UPDATE SupplierOrderItem s SET s.unitPrice = ?3 WHERE s.orderId = ?1 AND productId = ?2")
    int updatePriceByOrderIdAndProductId(String orderId, String productId, BigDecimal price);

    @Modifying
    int deleteByOrderId(String orderId);
}
