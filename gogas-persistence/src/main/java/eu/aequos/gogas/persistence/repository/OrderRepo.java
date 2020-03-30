package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrderRepo extends CrudRepository<Order, String>, JpaSpecificationExecutor<Order> {

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.id = ?1")
    Optional<Order> findByIdWithType(String orderId);

    List<String> findByOrderTypeIdAndDueDateAndDeliveryDate(String orderType, LocalDate dueDate, LocalDate deliveryDate);

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

    @Query("SELECT o FROM Order o WHERE o.statusCode = 0 AND o.openingDate <= CURRENT_TIMESTAMP AND function('DateAdd', hh, o.dueHour, o.dueDate) >= CURRENT_TIMESTAMP")
    List<Order> getOpenOrders();

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t " +
            "WHERE function('DateAdd', hh, o.dueHour, o.dueDate) < CURRENT_TIMESTAMP AND o.deliveryDate >= function('convert', date, CURRENT_TIMESTAMP) " +
            "AND (t.external = true OR EXISTS (SELECT i.id FROM OrderItem i WHERE i.user = ?1 AND i.order = o.id))")
    List<Order> getInDeliveryOrders(String userId);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.attachmentType = ?2 WHERE o.id = ?1")
    int updateAttachmentType(String orderId, String contentType);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.externalOrderId = ?2, o.sent = ?3 WHERE o.id = ?1")
    int updateOrderExternalId(String orderId, String externalOrderId, boolean sent);

    @Modifying
    @Query("UPDATE Order o SET o.invoiceNumber = ?2, o.invoiceAmount = ?3, o.lastSynchro = ?4 WHERE o.id = ?1")
    int updateInvoiceDataAndSynchDate(String orderId, String invoiceNumber, BigDecimal orderTotalAmount, LocalDateTime lastSynchroDate);

    @Modifying
    @Query("UPDATE Order o SET o.lastWeightUpdate = ?2 WHERE o.id = ?1")
    int updateWeightSentDate(String orderId, LocalDateTime weightSentDate);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.statusCode = 2 AND o.invoiceDate BETWEEN ?1 AND ?2")
    List<Order> findAccountedByInvoiceDateBetween(LocalDate invoiceDateFrom, LocalDate invoiceDateTo);

    @Modifying
    @Query("UPDATE Order set shippingCost = 0 WHERE id = ?1")
    int clearShippingCost(String orderId);
}
