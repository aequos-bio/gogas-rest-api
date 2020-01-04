package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface OrderTypeRepo extends CrudRepository<OrderType, String> {

    List<OrderType> findAllByOrderByDescription();

    List<OrderType> findByIdInOrderByDescription(List<String> orderIds);

    List<OrderType> findByAequosOrderIdNotNull();

    @Modifying
    @Query("UPDATE OrderType t SET t.lastsynchro = ?2 WHERE t.id = ?1")
    int setLastSynchroById(String id, Date lastSynchro);
}
