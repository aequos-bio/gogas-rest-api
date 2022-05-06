package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.ProductCategory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepo extends CrudRepository<ProductCategory, String> {

    Optional<ProductCategory> findByIdAndOrderTypeId(String id, String orderTypeId);

    List<ProductCategory> findByOrderTypeId(String orderTypeId);

    Optional<ProductCategory> findByOrderTypeIdAndDescription(String orderTypeId, String description);

    @Query("SELECT c FROM ProductCategory c, Order o " +
            "WHERE c.orderTypeId = o.orderType.id AND o.id = ?1 " +
            "AND EXISTS (SELECT p.id FROM Product p WHERE p.category.id = c.id AND p.available = true) " +
            "ORDER BY c.priceListPosition")
    List<ProductCategory> findByOrderId(String orderId);
}
