package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AccountingRepo extends CrudRepository<AccountingEntry, String>, JpaSpecificationExecutor<AccountingEntry> {

    List<AccountingEntry> findByOrderId(String orderId);

    Optional<AccountingEntry> findByOrderIdAndUserId(String orderId, String userId);

    long countByOrderId(String orderId);

    @Modifying
    @Query("UPDATE AccountingEntry a SET a.confirmed = ?2 WHERE a.orderId = ?1")
    int setConfirmedByOrderId(String orderId, boolean confirmed);

    @Transactional
    @Modifying
    @Query("DELETE AccountingEntry a WHERE a.orderId = ?1 AND a.user.id = ?2 AND confirmed = false")
    int deleteByOrderIdAndUserId(String orderId, String userId);
}
