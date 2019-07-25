package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepo extends CrudRepository<Order, String> {

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.orderType t LEFT JOIN FETCH o.orderSummaries s LEFT JOIN FETCH s.user u WHERE (u.id = ?1 OR u IS NULL) AND CURRENT_TIMESTAMP BETWEEN o.openingDate AND o.dueDate")
    List<Order> openOrders(String userId);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.id = ?1")
    Optional<Order> findByIdWithType(String orderId);
}
