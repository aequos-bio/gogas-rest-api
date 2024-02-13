package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OrderTotal;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummaryDerived;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummaryExtraction;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserOrderSummaryRepo extends CrudRepository<UserOrderSummary, UserOrderSummary.Key> {

    @Query("SELECT o FROM UserOrderSummary o WHERE o.userId = ?1 AND o.orderId IN ?2")
    Optional<UserOrderSummary> findUserOrderSummaryByUser(String userId, Set<String> orderIds);

    @Query(value = "SELECT su.orderId, su.itemsCount, COALESCE(sf.friendCount, 0) as friendCount, COALESCE(sf.friendAccounted, 0) as friendAccounted, su.totalAmount + COALESCE(st.importo, 0) as totalAmount " +
            "FROM UserOrderSummary su " +
            "LEFT OUTER JOIN (SELECT orderId, friendReferralId, SUM(itemsCount) AS friendCount, SUM(accountedItemsCount) AS friendAccounted " +
            "                 FROM UserOrderSummary sf " +
            "                 GROUP BY friendReferralId, orderId) sf ON su.userId = sf.friendReferralId AND su.orderId = sf.orderId " +
            "LEFT OUTER JOIN speseTrasporto st ON su.userId = st.idUtente AND su.orderId = st.idDateOrdini " +
            "WHERE su.userId = ?1 AND su.orderId IN ?2", nativeQuery = true)
    List<UserOrderSummaryDerived> findUserAndFriendOrderSummaryByUser(String userId, Set<String> orderIds);

    @Query("SELECT o FROM UserOrderSummary o WHERE o.orderId IN ?1")
    List<UserOrderSummary> findUserOrderSummaryByOrder(String orderId);

    @Query("SELECT o FROM UserOrderSummary o WHERE o.orderId IN ?1 AND o.aggregated = true")
    List<UserOrderSummary> findAggregatedUserOrderSummaryByOrder(String orderId);

    @Query("SELECT o FROM UserOrderSummary o WHERE o.orderId IN ?1 AND o.aggregated = true AND o.totalAmount IS NOT NULL")
    List<UserOrderSummary> findAccountableUserOrderSummaryByOrder(String orderId);

    @Query("SELECT o FROM UserOrderSummary o WHERE o.orderId IN ?1 AND o.userId IN ?2")
    List<UserOrderSummary> findUserOrderSummaryByOrder(String orderId, Set<String> userIds);

    @Query("SELECT o FROM UserOrderSummary o JOIN User u ON o.userId = u.id " +
            "WHERE (u.id = ?1 OR u.friendReferral.id = ?1) AND o.orderId IN ?2")
    List<UserOrderSummary> findOpenOrderSummaries(String userId, Set<String> orderIds);

    @Query(value = "SELECT o.orderId as orderId, SUM(COALESCE(o.totalAmount, 0)) AS totalAmount " +
            "FROM UserOrderSummary o " +
            "WHERE o.orderId IN ?1 AND o.aggregated = 1 " +
            "GROUP BY o.orderId")
    List<OrderSummary> getOrdersTotal(Set<String> orderIds);

   @Query(value = "SELECT d.idDateOrdini as orderId, t.tipoOrdine as description, d.dataConsegna as deliveryDate, " +
            "COALESCE((SELECT SUM(s.totalAmount) FROM userOrderSummary s WHERE s.orderId = d.idDateOrdini), 0) as total " +
            "FROM dateordini d " +
            "    inner join tipologiaOrdine t on d.idTipologiaOrdine = t.idTipologiaOrdine " +
            "WHERE d.stato = 2 " +
            "AND d.dataConsegna >= ?1 " +
            "AND d.dataConsegna < ?2 " +
            "order by d.dataConsegna ", nativeQuery = true)
    List<OrderTotal> getOrderTotals(LocalDate deliveryDateFrom, LocalDate deliveryDateTo);

    @Query(value = "SELECT " +
            "   tot.idUtente as userId,\n" +
            "   tot.idReferenteAmico as friendReferralId,\n" +
            "   CASE\n" +
            "      WHEN t.\"external\" = 1 OR t.totaleCalcolato = 0 THEN null\n" +
            "      ELSE tot.importo\n" +
            "   END as totalAmount,\n" +
            "   tot.itemsCount,\n" +
            "   tot.accountedItemsCount,\n" +
            "   CAST(d.stato AS BIT) as aggregated\n" +
            "FROM dateOrdini d\n" +
            "INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine\n" +
            "INNER JOIN (\n" +
            "    SELECT idDateOrdine, idUtente, idReferenteAmico, riepilogoUtente,\n" +
            "            ROUND(SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END), 2) importo,\n" +
            "            COUNT(idRigaOrdine) itemsCount," +
            "            SUM(CAST(contabilizzato AS int)) accountedItemsCount\n" +
            "    FROM ordini o\n" +
            "    INNER JOIN prodotti p ON o.idProdotto = p.idProdotto\n" +
            "    WHERE idUtente = ?2\n" +
            "    GROUP BY idDateOrdine, idUtente, idReferenteAmico, riepilogoUtente\n" +
            ") as tot ON d.idDateOrdini = tot.idDateOrdine AND tot.riepilogoUtente = CAST(d.stato AS BIT)\n" +
            "WHERE d.idDateOrdini = ?1", nativeQuery = true)
    Optional<UserOrderSummaryExtraction> extractUserOrderSummary(String orderId, String userId);

    @Query(value = "SELECT " +
            "   tot.idUtente as userId,\n" +
            "   tot.idReferenteAmico as friendReferralId,\n" +
            "   CASE\n" +
            "      WHEN t.\"external\" = 1 OR t.totaleCalcolato = 0 THEN null\n" +
            "      ELSE tot.importo\n" +
            "   END as totalAmount,\n" +
            "   tot.itemsCount,\n" +
            "   tot.accountedItemsCount,\n" +
            "   CAST(d.stato AS BIT) as aggregated\n" +
            "FROM dateOrdini d\n" +
            "INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine\n" +
            "INNER JOIN (\n" +
            "    SELECT idDateOrdine, idUtente, idReferenteAmico, riepilogoUtente,\n" +
            "            ROUND(SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END), 2) importo,\n" +
            "            COUNT(idRigaOrdine) itemsCount," +
            "            SUM(CAST(contabilizzato AS int)) accountedItemsCount\n" +
            "    FROM ordini o\n" +
            "    INNER JOIN prodotti p ON o.idProdotto = p.idProdotto\n" +
            "    GROUP BY idDateOrdine, idUtente, idReferenteAmico, riepilogoUtente\n" +
            ") as tot ON d.idDateOrdini = tot.idDateOrdine AND tot.riepilogoUtente = CAST(d.stato AS BIT)\n" +
            "WHERE d.idDateOrdini = ?1", nativeQuery = true)
    List<UserOrderSummaryExtraction> extractUserOrderSummaries(String orderId);

    @Query(value = "SELECT COUNT(idRigaOrdine) itemsCount " +
            "FROM ordini o WHERE idDateOrdine = ?1 AND idUtente = ?2 AND riepilogoUtente = 1", nativeQuery = true)
    Optional<Integer> countUserOrderItemsForNotComputedOrder(String orderId, String userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserOrderSummary s WHERE s.orderId = ?1 AND s.userId = ?2")
    int deleteByOrderIdAndUserId(String orderId, String userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserOrderSummary s SET s.totalAmount = null WHERE s.orderId = ?1 AND s.userId = ?2")
    int clearByOrderIdAndUserId(String orderId, String userId);

    @Modifying
    @Query("DELETE FROM UserOrderSummary WHERE orderId = ?1")
    int deleteAllUserOrderSummary(String orderId);
}
