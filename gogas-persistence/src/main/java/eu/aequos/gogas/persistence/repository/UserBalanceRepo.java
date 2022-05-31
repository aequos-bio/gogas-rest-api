package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.UserBalance;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserBalanceRepo extends CrudRepository<UserBalance, String> {

    @Procedure(name = "User.balance")
    Optional<BigDecimal> getBalance(@Param("idUtente") String userId);

    List<UserBalance> findAllByRole(String role);

    List<UserBalance> findByReferralId(String referralId);
}
