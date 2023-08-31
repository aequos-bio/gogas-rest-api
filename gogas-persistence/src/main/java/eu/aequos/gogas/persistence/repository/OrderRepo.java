package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.OrderUserTotal;
import org.springframework.data.domain.Pageable;
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

    List<Order> findByIdIn(Set<String> orderIds);

    List<Order> findByOrderTypeIdAndDueDateAndDeliveryDate(String orderType, LocalDate dueDate, LocalDate deliveryDate);

    @Query("SELECT DISTINCT LOWER(o.orderType.id) FROM Order o")
    Set<String> findAllUsedOrderTypes();

    List<Order> findByOrderTypeIdInAndStatusCodeIn(Set<String> orderTypeId, Set<Integer> statusCodes);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.dueDate >= ?1")
    List<Order> findByDueDateGreaterThanEqual(LocalDate date);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.deliveryDate >= ?1")
    List<Order> findByDeliveryDateGreaterThanEqual(LocalDate date);

    @Modifying
    @Query("UPDATE Order o SET o.statusCode = ?2 WHERE o.id = ?1")
    int updateOrderStatus(String orderId, int status);

    @Query("SELECT o FROM Order o WHERE o.statusCode = 0 AND o.openingDate <= CURRENT_TIMESTAMP AND function('DateAdd', hh, o.dueHour, o.dueDate) >= CURRENT_TIMESTAMP ORDER BY o.dueDate")
    List<Order> getOpenOrders();

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t " +
            "WHERE function('DateAdd', hh, o.dueHour, o.dueDate) < CURRENT_TIMESTAMP AND o.deliveryDate >= function('convert', date, CURRENT_TIMESTAMP) " +
            "AND (t.external = true OR EXISTS (SELECT i.id FROM OrderItem i WHERE i.user = ?1 AND i.order = o.id)) " +
            "ORDER BY o.deliveryDate ASC, t.description ASC")
    List<Order> getInDeliveryOrders(String userId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.attachmentType = ?2 WHERE o.id = ?1")
    int updateAttachmentType(String orderId, String contentType);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.externalOrderId = ?2, o.sent = ?3 WHERE o.id = ?1")
    int updateOrderExternalId(String orderId, String externalOrderId, boolean sent);

    @Modifying
    @Query("UPDATE Order o SET o.invoiceNumber = ?2, o.invoiceAmount = ?3, o.invoiceDate = ?4, o.lastSynchro = ?5 WHERE o.id = ?1")
    int updateInvoiceDataAndSynchDate(String orderId, String invoiceNumber, BigDecimal orderTotalAmount, LocalDate invoiceDate, LocalDateTime lastSynchroDate);

    @Modifying
    @Query("UPDATE Order o SET o.lastWeightUpdate = ?2 WHERE o.id = ?1")
    int updateWeightSentDate(String orderId, LocalDateTime weightSentDate);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.statusCode = 2 AND o.invoiceDate BETWEEN ?1 AND ?2")
    List<Order> findAccountedByInvoiceDateBetween(LocalDate invoiceDateFrom, LocalDate invoiceDateTo);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t WHERE o.statusCode = 2 AND (o.invoiceNumber IS NULL OR o.invoiceDate IS NULL OR o.invoiceAmount IS NULL) AND o.deliveryDate BETWEEN ?1 AND ?2 AND o.orderType.billedByAequos=?3")
    List<Order> findAccountedOrdersWithoutInvoice(LocalDate invoiceDateFrom, LocalDate invoiceDateTo, boolean aequosOrders);

    @Query(value = "SELECT o.idUtente as userId, o.idReferenteAmico as friendReferralId, o.importo as amount, s.importo AS shippingCost " +
            "FROM  " +
            "(SELECT o.idUtente, o.idReferenteAmico, ROUND(SUM(o.qtaRitirataKg * o.prezzoKg), 2) as importo " +
            " FROM ordini o " +
            " WHERE o.riepilogoUtente = 0 AND contabilizzato = 1 AND o.idReferenteAmico = ?1 AND o.idDateOrdine = ?2 " +
            " GROUP BY o.idUtente, o.idReferenteAmico " +
            ") o " +
            "LEFT OUTER JOIN speseTrasporto s ON s.idDateOrdini = ?2 AND s.idUtente = o.idUtente ", nativeQuery = true)
    List<OrderUserTotal> getComputedOrderTotalsForFriendAccounting(String friendReferralId, String orderId);
}
