package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.UserBalance;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface UserBalanceRepo extends CrudRepository<UserBalance, String> {

    @Procedure(name = "User.balance")
    BigDecimal getBalance(@Param("idUtente") String userId);

    @Override
    List<UserBalance> findAll();

    List<UserBalance> findByReferralId(String referralId);
}
