package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderTypeRepo extends CrudRepository<OrderType, String> {

    List<OrderType> findAllByOrderByDescription();

    List<OrderType> findByIdInOrderByDescription(List<String> orderIds);

    List<OrderType> findByAequosOrderIdNotNull();

    @Query("SELECT t FROM OrderType t WHERE t.billedByAequos = false")
    List<OrderType> findOrderTypesNotBilledByAequos();

    @Query("SELECT DISTINCT t.accountingCode FROM OrderType t WHERE t.billedByAequos = true")
    String findAequosAccountingCode();

    List<OrderType> findByHasTurns(boolean hasTurns);

    @Modifying
    @Query("UPDATE OrderType t SET t.accountingCode = ?1 WHERE t.billedByAequos = true")
    int updateAequosAccountingCode(String accountingCode);

    @Modifying
    @Query("UPDATE OrderType t SET t.accountingCode = ?2 WHERE t.id = ?1")
    int updateAccountingCode(String orderTypeId, String accountingCode);

    @Modifying
    @Query("UPDATE OrderType t SET t.lastsynchro = ?2 WHERE t.id = ?1")
    int setLastSynchroById(String id, LocalDateTime lastSynchro);
}
