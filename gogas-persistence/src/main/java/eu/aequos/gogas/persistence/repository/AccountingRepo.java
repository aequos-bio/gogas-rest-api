package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingRepo extends CrudRepository<AccountingEntry, String>, JpaSpecificationExecutor<AccountingEntry> {

    List<AccountingEntry> findByOrderId(String orderId);

    Optional<AccountingEntry> findByOrderIdAndUserId(String orderId, String userId);

    List<AccountingEntry> findByOrderIdAndFriendReferralId(String orderId, String friendReferralId);

    List<AccountingEntry> findByUserId(String userId);
}
