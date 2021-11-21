package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.ProductCategory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepo extends CrudRepository<ProductCategory, String> {

    List<ProductCategory> findByOrderTypeId(String orderTypeId);

    Optional<ProductCategory> findByOrderTypeIdAndDescription(String orderTypeId, String description);

    @Query("SELECT c FROM ProductCategory c, Order o WHERE c.orderTypeId = o.orderType.id AND o.id = ?1 ORDER BY c.priceListPosition")
    List<ProductCategory> findByOrderId(String orderId);
}
