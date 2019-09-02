package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Product;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductRepo extends CrudRepository<Product, String>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p JOIN FETCH p.category c JOIN FETCH p.supplier s WHERE p.type = ?1")
    List<Product> findByType(String type);

    @Query("SELECT p FROM Product p JOIN FETCH p.category c WHERE p.available = true and p.type = ?1 ORDER BY c.priceListPosition, p.description")
    List<Product> findAvailableByTypeOrderByPriceList(String type);

    @Query("SELECT p FROM Product p JOIN FETCH p.category c JOIN FETCH p.supplier s WHERE p.id IN (?1) ORDER BY c.priceListPosition, p.description")
    List<Product> findByIdInOrderByPriceList(Set<String> productIds);

    List<Product> findByIdIn(Set<String> productIds);

    Optional<Product> findByExternalId(String externalId);

    @Modifying
    @Query("UPDATE Product p SET p.available = false WHERE p.type = ?1 AND p.id NOT IN (?2)")
    int setNotAvailableByTypeAndIdNotIn(String typeId, Set<String> ids);
}
