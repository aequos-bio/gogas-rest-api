package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AuditUserBalance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AuditUserBalanceRepo extends CrudRepository<AuditUserBalance, AuditUserBalance.Key> {

    List<AuditUserBalance> findAllByOrderByTs();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM auditUserBalance", nativeQuery = true)
    void clearAll();
}
