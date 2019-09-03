package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserSummary;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepo extends CrudRepository<User, String> {

    @Override
    List<User> findAll();

    List<User> findByFirstNameContainingAndLastNameContaining(String firstName, String lastName);

    Optional<User> findByUsername(String username);

    <T> List<T> findByRole(String role, Class<T> type);

    List<UserCoreInfo> findByRoleInAndEnabled(Set<String> roles, boolean enabled);

    List<User> findByRole(String role);

    <T> List<T> findByIdIn(Set<String> usrIds, Class<T> type);

    List<UserCoreInfo> findByIdNotInAndRoleInAndEnabled(Set<String> usrIds, Set<String> roles, boolean enabled);

    List<UserSummary> findByFriendReferralId(String referralId);

    boolean existsUserByIdAndFriendReferralId(String userId, String frientReferrald);

    @Modifying
    @Query(value = "UPDATE utenti SET nome = :name WHERE idutente = :id", nativeQuery = true)
    int updateFirstName(@Param("id") String id, @Param("name") String name);

    @Procedure(name = "UserExport.balance")
    BigDecimal getBalance(@Param("idUtente") String userId);
}
