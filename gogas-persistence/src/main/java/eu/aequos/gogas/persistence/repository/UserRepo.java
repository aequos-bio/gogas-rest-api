package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

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

    List<UserCoreInfo> findByIdNotInAndRoleInAndEnabled(Set<String> usrIds, Set<String> roles, boolean enabled);

    boolean existsUserByIdAndFriendReferralId(String userId, String frientReferrald);

    @Query("SELECT u.balance FROM User u WHERE u.id = ?1")
    BigDecimal getBalance(String userId);

    @Modifying
    @Query(value = "UPDATE User u SET u.password = ?2 WHERE id = ?1")
    int updatePassword(String userId, String encodedPassword);

    User findByUsernameAndEmail(String username, String email);

    @Modifying
    @Query("UPDATE User u SET u.balance = u.balance + ?2 WHERE u.id = ?1")
    int updateBalance(String userId, BigDecimal amount);

    @Modifying
    @Query(value = "WITH mov AS ( " +
            "   SELECT idUtente, idReferente, importo * CASE WHEN confermato = 1 THEN 1 ELSE -1 END as importo " +
            "   FROM movimenti m " +
            "   WHERE m.idDateOrdini = ?1 " +
            ") " +
            "UPDATE u SET u.balance = u.balance - (mu.importo + COALESCE(ma.importo, 0)) " +
            "FROM utenti u " +
            "INNER JOIN mov mu ON u.idUtente = mu.idUtente " +
            "LEFT JOIN mov ma ON u.idUtente = ma.idReferente", nativeQuery = true)
    int updateBalancesFromAccountingEntriesByOrderId(String orderId);

    @Modifying
    @Query(value = "WITH mov AS ( " +
            "   SELECT idUtente, idReferenteAmico, SUM(qtaRitirataKg * prezzoKg * CASE WHEN contabilizzato = 0 THEN -1 ELSE 1 END) as importo " +
            "   FROM ordini " +
            "   WHERE idDateOrdine = ?1 and riepilogoUtente = 1 " +
            "   GROUP BY idUtente, idReferenteAmico " +
            ") " +
            "UPDATE u SET u.balance = u.balance - (mu.importo + COALESCE(ma.importo, 0)) " +
            "FROM utenti u " +
            "INNER JOIN mov mu ON u.idUtente = mu.idUtente " +
            "LEFT JOIN mov ma ON u.idUtente = ma.idReferenteAmico", nativeQuery = true)
    int updateBalancesFromOrderItemsByOrderId(String orderId);

    @Modifying
    @Query(value = "UPDATE u SET u.balance = u.balance - (o.qtaRitirataKg * o.prezzoKg * CASE WHEN o.contabilizzato = 0 THEN -1 ELSE 1 END) " +
            "FROM utenti u " +
            "INNER JOIN ordini o ON u.idUtente = o.idUtente " +
            "WHERE o.idDateOrdine = ?2 AND o.idProdotto = ?3 AND o.idReferenteAmico = ?1 AND o.riepilogoUtente = 0", nativeQuery = true)
    int updateFriendBalancesFromOrderItems(String referralId, String orderId, String productId);
}
