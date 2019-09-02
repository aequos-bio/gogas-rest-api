package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
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

    @Modifying
    @Query("UPDATE Order o SET o.statusCode = ?2 WHERE o.id = ?1")
    int updateOrderStatus(String orderId, int status);
}
