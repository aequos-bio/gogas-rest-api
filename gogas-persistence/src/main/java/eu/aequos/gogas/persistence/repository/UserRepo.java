package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserSummary;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface UserRepo extends CrudRepository<User, String> {

    @Override
    List<User> findAll();

    List<User> findByFirstNameContainingAndLastNameContaining(String firstName, String lastName);

    <T> List<T> findByRole(String role, Class<T> type);

    List<User> findByRole(String role);

    List<User> findByIdIn(Set<String> usrIds);

    List<UserSummary> findByFriendReferralId(String referralId);

    @Modifying
    @Query(value = "UPDATE utenti SET nome = :name WHERE idutente = :id", nativeQuery = true)
    int updateFirstName(@Param("id") String id, @Param("name") String name);

    @Procedure(name = "UserExport.balance")
    BigDecimal getBalance(@Param("idUtente") String userId);
}
