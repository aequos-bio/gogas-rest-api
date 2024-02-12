package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderUserBlacklist;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderUserBlacklistRepo extends CrudRepository<OrderUserBlacklist, OrderUserBlacklist.Key> {

    @Query("SELECT b.userId FROM OrderUserBlacklist b WHERE b.orderTypeId = ?1")
    List<String> getUserIdsByOrderId(String orderTypeId);

    @Query("SELECT b.orderTypeId FROM OrderUserBlacklist b WHERE b.userId = ?1")
    List<String> getOrderIdsByUserId(String userId);

    @Modifying
    @Query("DELETE FROM OrderUserBlacklist b WHERE b.userId = ?1")
    void deleteByUserId(String userId);
}
