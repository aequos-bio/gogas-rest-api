package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.ProductCategory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepo extends CrudRepository<ProductCategory, String> {

    List<ProductCategory> findByOrderTypeId(String orderTypeId);

    Optional<ProductCategory> findByOrderTypeIdAndDescription(String orderTypeId, String description);
}
