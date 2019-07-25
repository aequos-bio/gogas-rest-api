package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.ProductCategory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductCategoryRepo extends CrudRepository<ProductCategory, String> {

    List<ProductCategory> findByOrderTypeId(String orderTypeId);
}
