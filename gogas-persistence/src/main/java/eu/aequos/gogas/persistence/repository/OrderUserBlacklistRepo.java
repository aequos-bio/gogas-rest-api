package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderUserBlacklist;
import eu.aequos.gogas.persistence.entity.derived.ByUserBlacklistCount;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderUserBlacklistRepo extends CrudRepository<OrderUserBlacklist, OrderUserBlacklist.Key> {

    @Query("SELECT b.userId FROM OrderUserBlacklist b WHERE b.orderTypeId = ?1")
    List<String> getUserIdsByOrderId(String orderTypeId);

    @Query("SELECT b.orderTypeId FROM OrderUserBlacklist b WHERE b.userId = ?1")
    List<String> getOrderIdsByUserId(String userId);

    @Query("SELECT b.userId as userId, COUNT(b.orderTypeId) as blacklistEntriesCount FROM OrderUserBlacklist b GROUP BY b.userId")
    List<ByUserBlacklistCount> countOrderIdsByUser();

    @Modifying
    @Query("DELETE FROM OrderUserBlacklist b WHERE b.userId = ?1")
    void deleteByUserId(String userId);
}
