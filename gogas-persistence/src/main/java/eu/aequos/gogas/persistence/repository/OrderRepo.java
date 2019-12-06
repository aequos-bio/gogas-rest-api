package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.ByUserOrderItem;
import eu.aequos.gogas.persistence.entity.derived.FriendTotalOrder;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummary;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.*;

public interface OrderRepo extends CrudRepository<Order, String>, JpaSpecificationExecutor<Order> {

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.id = ?1")
    Optional<Order> findByIdWithType(String orderId);

    List<String> findByOrderTypeIdAndDueDateAndDeliveryDate(String orderType, Date dueDate, Date deliveryDate);

    @Query("SELECT DISTINCT o.orderType.id FROM Order o")
    Set<String> findAllUsedOrderTypes();

    List<Order> findByOrderTypeIdInAndStatusCodeIn(Set<String> orderTypeId, Set<Integer> statusCodes);

    @Query(value = "SELECT idDateOrdini AS orderId, " +
            "CASE " +
            "   WHEN t.\"external\" = 1 OR t.totaleCalcolato = 0 THEN " +
            "       (SELECT SUM(importo) FROM movimenti m WHERE d.idDateOrdini = m.idDateOrdini) " +
            "   ELSE " +
            "       (SELECT SUM(qtaRitirataKg * prezzoKg) FROM ordini o WHERE d.idDateOrdini = o.idDateOrdine AND riepilogoUtente = 1) " +
            "END as totalAmount " +
            "FROM dateOrdini d " +
            "INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine " +
            "WHERE d.idDateOrdini IN ?1", nativeQuery = true)
    List<OrderSummary> findOrderSummary(Set<String> orderIds);

    //TODO: semplificare salvando totali nel db
    @Query(value = "SELECT idDateOrdini AS orderId, " +
            "CASE " +
            "   WHEN t.\"external\" = 1 OR t.totaleCalcolato = 0 THEN " +
            "       (SELECT SUM(importo) FROM movimenti m WHERE d.idDateOrdini = m.idDateOrdini AND m.idUtente = ?1) " +
            "   ELSE " +
            "       (SELECT SUM(qtaRitirataKg * prezzoKg) FROM ordini o WHERE d.idDateOrdini = o.idDateOrdine AND o.riepilogoUtente = CAST(d.stato AS BIT) AND o.idUtente = ?1) " +
            "END as totalAmount, " +
            "(SELECT COUNT(*) FROM ordini o WHERE d.idDateOrdini = o.idDateOrdine AND o.riepilogoUtente = CAST(d.stato AS BIT) AND o.idUtente = ?1) as itemsCount, " +
            "f.friendCount, f.friendAccounted " +
            "FROM dateOrdini d " +
            "INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine " +
            "INNER JOIN (" +
            "  SELECT o.idDateOrdine, COUNT(*) AS friendCount, " +
            "  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted " +
            "  FROM ordini o WHERE idReferenteAmico = ?1 AND o.riepilogoUtente = 0 " +
            "  GROUP BY o.idDateOrdine " +
            ") f ON d.idDateOrdini = f.idDateOrdine " +
            "WHERE d.idDateOrdini IN ?2", nativeQuery = true)
    List<UserOrderSummary> findUserOrderSummary(String userId, Set<String> orderIds);

    @Modifying
    @Query("UPDATE Order o SET o.statusCode = ?2 WHERE o.id = ?1")
    int updateOrderStatus(String orderId, int status);
}
