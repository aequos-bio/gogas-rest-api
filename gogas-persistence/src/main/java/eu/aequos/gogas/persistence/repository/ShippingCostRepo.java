package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.ShippingCost;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ShippingCostRepo extends CrudRepository<ShippingCost, ShippingCost.Key> {

    List<ShippingCost> findByOrderId(String orderId);

    @Modifying
    @Query("DELETE ShippingCost s WHERE orderId = ?1 AND userId = ?2")
    int deleteByOrderIdAndUserId(String orderId, String userId);
}
