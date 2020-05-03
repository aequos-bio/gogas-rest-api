package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummaryExtraction;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserOrderSummaryRepo extends CrudRepository<UserOrderSummary, UserOrderSummary.Key> {

    @Query("SELECT o FROM UserOrderSummary o WHERE o.userId = ?1 AND o.orderId IN ?2")
    List<UserOrderSummary> findUserOrderSummaryByUser(String userId, Set<String> orderIds);

    @Query("SELECT o FROM UserOrderSummary o WHERE o.orderId IN ?1")
    List<UserOrderSummary> findUserOrderSummaryByOrder(String orderId);

    @Query("SELECT o FROM UserOrderSummary o WHERE o.orderId IN ?1 AND o.userId IN ?2")
    List<UserOrderSummary> findUserOrderSummaryByOrder(String orderId, Set<String> userIds);

    @Query("SELECT o FROM UserOrderSummary o JOIN User u ON o.userId = u.id " +
            "WHERE (u.id = ?1 OR u.friendReferral.id = ?1) AND o.orderId IN ?2")
    List<UserOrderSummary> findOpenOrderSummaries(String userId, Set<String> orderIds);

    @Query(value = "SELECT tot.idUtente as userId, tot.importo as totalAmount, tot.itemsCount,\n" +
            "   COALESCE(f.friendCount, 0) as friendItemsCount,\n" +
            "   COALESCE(f.friendAccounted, 0) as friendItemsAccounted,\n" +
            "   COALESCE(s.importo, 0) AS shippingCost\n" +
            "FROM dateOrdini d\n" +
            "INNER JOIN (\n" +
            "    SELECT idDateOrdine, idUtente, riepilogoUtente,\n" +
            "            SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END) importo,\n" +
            "            COUNT(idRigaOrdine) itemsCount\n" +
            "    FROM ordini o\n" +
            "    INNER JOIN prodotti p ON o.idProdotto = p.idProdotto\n" +
            "    WHERE idUtente = ?2\n" +
            "    GROUP BY idDateOrdine, idUtente, riepilogoUtente\n" +
            ") as tot ON d.idDateOrdini = tot.idDateOrdine AND tot.riepilogoUtente = CAST(d.stato AS BIT)\n" +
            "LEFT OUTER JOIN speseTrasporto s ON  tot.idDateOrdine = s.idDateOrdini AND s.idUtente = tot.idUtente\n" +
            "LEFT OUTER JOIN (\n" +
            "  SELECT o.idDateOrdine, o.idReferenteAmico, COUNT(*) AS friendCount,\n" +
            "  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted\n" +
            "  FROM ordini o\n" +
            "  WHERE o.riepilogoUtente = 0 AND o.idReferenteAmico IS NOT NULL\n" +
            "  GROUP BY o.idDateOrdine, o.idReferenteAmico\n" +
            ") f ON f.idDateOrdine = tot.idDateOrdine AND f.idReferenteAmico = tot.idUtente\n" +
            "WHERE d.idDateOrdini = ?1", nativeQuery = true)
    Optional<UserOrderSummaryExtraction> extractUserOrderSummary(String orderId, String userId);

    @Query(value = "SELECT tot.idUtente as userId, tot.importo as totalAmount, tot.itemsCount,\n" +
            "   COALESCE(f.friendCount, 0) as friendItemsCount,\n" +
            "   COALESCE(f.friendAccounted, 0) as friendItemsAccounted,\n" +
            "   COALESCE(s.importo, 0) AS shippingCost\n" +
            "FROM dateOrdini d\n" +
            "INNER JOIN (\n" +
            "    SELECT idDateOrdine, idUtente, riepilogoUtente,\n" +
            "            SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END) importo,\n" +
            "            COUNT(idRigaOrdine) itemsCount\n" +
            "    FROM ordini o\n" +
            "    INNER JOIN prodotti p ON o.idProdotto = p.idProdotto\n" +
            "    GROUP BY idDateOrdine, idUtente, riepilogoUtente\n" +
            ") as tot ON d.idDateOrdini = tot.idDateOrdine AND tot.riepilogoUtente = CAST(d.stato AS BIT)\n" +
            "LEFT OUTER JOIN speseTrasporto s ON  tot.idDateOrdine = s.idDateOrdini AND s.idUtente = tot.idUtente\n" +
            "LEFT OUTER JOIN (\n" +
            "  SELECT o.idDateOrdine, o.idReferenteAmico, COUNT(*) AS friendCount,\n" +
            "  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted\n" +
            "  FROM ordini o\n" +
            "  WHERE o.riepilogoUtente = 0 AND o.idReferenteAmico IS NOT NULL\n" +
            "  GROUP BY o.idDateOrdine, o.idReferenteAmico\n" +
            ") f ON f.idDateOrdine = tot.idDateOrdine AND f.idReferenteAmico = tot.idUtente\n" +
            "WHERE d.idDateOrdini = ?1", nativeQuery = true)
    List<UserOrderSummaryExtraction> extractUserOrderSummaries(String orderId);

    @Query(value = "SELECT COALESCE(tot.idUtente, c.idUtente) as userId,\n" +
            "   tot.importo as totalAmount, COALESCE(c.itemsCount, 0) as itemsCount,\n" +
            "   COALESCE(f.friendCount, 0) as friendItemsCount,\n" +
            "   COALESCE(f.friendAccounted, 0) as friendItemsAccounted,\n" +
            "   COALESCE(s.importo, 0) AS shippingCost\n" +
            "FROM dateOrdini d\n" +
            "LEFT OUTER JOIN (\n" +
            "    SELECT idDateOrdini, idUtente, SUM(importo) as importo FROM movimenti m " +
            "    WHERE m.idDateOrdini IS NOT NULL AND idUtente = ?2 \n" +
            "    GROUP BY idDateOrdini, idUtente \n" +
            ") as tot ON d.idDateOrdini = tot.idDateOrdini \n" +
            "LEFT OUTER JOIN ( \n" +
            "    SELECT idDateOrdine, idUtente, COUNT(idRigaOrdine) itemsCount FROM ordini o WHERE idUtente = ?2 AND riepilogoUtente = 1 GROUP BY idDateOrdine, idUtente \n" +
            ") as c ON d.idDateOrdini = c.idDateOrdine \n" +
            "LEFT OUTER JOIN speseTrasporto s ON  tot.idDateOrdini = s.idDateOrdini AND s.idUtente = tot.idUtente \n" +
            "LEFT OUTER JOIN (\n" +
            "  SELECT o.idDateOrdine, o.idReferenteAmico, COUNT(*) AS friendCount,\n" +
            "  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted\n" +
            "  FROM ordini o\n" +
            "  WHERE o.riepilogoUtente = 0 AND o.idReferenteAmico IS NOT NULL\n" +
            "  GROUP BY o.idDateOrdine, o.idReferenteAmico\n" +
            ") f ON f.idDateOrdine = tot.idDateOrdini AND f.idReferenteAmico = tot.idUtente\n" +
            "WHERE d.idDateOrdini = ?1 AND COALESCE(tot.idUtente, c.idUtente) IS NOT NULL", nativeQuery = true)
    Optional<UserOrderSummaryExtraction> extractUserOrderSummaryFromAccountEntry(String orderId, String userId);

    @Query(value = "SELECT c.idUtente as userId,\n" +
            "   tot.importo as totalAmount, COALESCE(c.itemsCount, 0) as itemsCount,\n" +
            "   COALESCE(f.friendCount, 0) as friendItemsCount,\n" +
            "   COALESCE(f.friendAccounted, 0) as friendItemsAccounted,\n" +
            "   COALESCE(s.importo, 0) AS shippingCost\n" +
            "FROM dateOrdini d\n" +
            "INNER JOIN ( \n" +
            "    SELECT idDateOrdine, idUtente, COUNT(idRigaOrdine) itemsCount FROM ordini o WHERE riepilogoUtente = 1 GROUP BY idDateOrdine, idUtente \n" +
            ") as c ON d.idDateOrdini = c.idDateOrdine \n" +
            "LEFT OUTER JOIN (\n" +
            "    SELECT idDateOrdini, idUtente, SUM(importo) as importo FROM movimenti m WHERE m.idDateOrdini IS NOT NULL \n" +
            "    GROUP BY idDateOrdini, idUtente \n" +
            ") as tot ON d.idDateOrdini = tot.idDateOrdini AND c.idUtente = tot.idUtente \n" +
            "LEFT OUTER JOIN speseTrasporto s ON  tot.idDateOrdini = s.idDateOrdini AND s.idUtente = tot.idUtente \n" +
            "LEFT OUTER JOIN (\n" +
            "  SELECT o.idDateOrdine, o.idReferenteAmico, COUNT(*) AS friendCount,\n" +
            "  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted\n" +
            "  FROM ordini o\n" +
            "  WHERE o.riepilogoUtente = 0 AND o.idReferenteAmico IS NOT NULL\n" +
            "  GROUP BY o.idDateOrdine, o.idReferenteAmico\n" +
            ") f ON f.idDateOrdine = tot.idDateOrdini AND f.idReferenteAmico = tot.idUtente\n" +
            "WHERE d.idDateOrdini = ?1", nativeQuery = true)
    List<UserOrderSummaryExtraction> extractUserOrderSummaryFromAccountEntries(String orderId);

    @Modifying
    @Query("UPDATE UserOrderSummary o SET o.shippingCost = ?3 WHERE o.orderId = ?1 AND o.userId = ?2")
    int updateShippingCost(String orderId, String userId, BigDecimal userShippingCost);

    @Modifying
    @Query("DELETE FROM UserOrderSummary WHERE orderId = ?1")
    int deleteAllUserOrderSummary(String orderId);
}
