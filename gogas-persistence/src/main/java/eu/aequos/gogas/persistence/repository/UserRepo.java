package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserSummary;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    <T> List<T> findByFriendReferralId(String friendReferralId, Class<T> type);

    List<UserCoreInfo> findByRoleInAndEnabled(Set<String> roles, boolean enabled);

    List<User> findByRole(String role);

    List<User> findByRoleIn(Set<String> roles);

    <T> List<T> findByIdIn(Set<String> usrIds, Class<T> type);

    List<UserCoreInfo> findByIdInAndRoleInAndEnabled(Set<String> usrIds, Set<String> roles, boolean enabled);
    List<UserCoreInfo> findByIdNotInAndRoleInAndEnabled(Set<String> usrIds, Set<String> roles, boolean enabled);

    boolean existsUserByIdAndFriendReferralId(String userId, String frientReferrald);

    @Query(value = "SELECT u FROM User u WHERE u.id = ?1 OR u.friendReferral = ?1")
    List<User> findUserAndFriendsByUserId(String userId);

    @Procedure(name = "UserExport.balance")
    BigDecimal getBalance(@Param("idUtente") String userId);

    @Modifying
    @Query(value = "UPDATE User u SET u.password = ?2 WHERE id = ?1")
    int updatePassword(String userId, String encodedPassword);

    User findByUsernameAndEmail(String username, String email);

    @Query(value = "SELECT max(u.position) FROM User u")
    int getMaxUserPosition();
}
