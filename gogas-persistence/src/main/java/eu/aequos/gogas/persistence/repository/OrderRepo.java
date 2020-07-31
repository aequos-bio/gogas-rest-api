package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OrderTotal;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummary;
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

    //TODO: semplificare salvando totali nel db
    @Query(value = "SELECT d.idDateOrdini AS orderId, " +
            "CASE " +
            "   WHEN t.\"external\" = 1 OR t.totaleCalcolato = 0 THEN " +
            "       (SELECT SUM(importo) FROM movimenti m WHERE d.idDateOrdini = m.idDateOrdini AND m.idUtente = ?1) " +
            "   ELSE " +
            "       (SELECT SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * " +
            "               CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END) " +
            "        FROM ordini o " +
            "        INNER JOIN prodotti p ON o.idProdotto = p.idProdotto " +
            "        WHERE d.idDateOrdini = o.idDateOrdine AND o.riepilogoUtente = CAST(d.stato AS BIT) AND o.idUtente = ?1" +
            "       ) " +
            "END as totalAmount, " +
            "(SELECT COUNT(*) FROM ordini o WHERE d.idDateOrdini = o.idDateOrdine AND o.riepilogoUtente = CAST(d.stato AS BIT) AND o.idUtente = ?1) as itemsCount, " +
            "COALESCE(f.friendCount, 0) as friendCount, COALESCE(f.friendAccounted, 0) as friendAccounted, " +
            "s.importo AS shippingCost " +
            "FROM dateOrdini d " +
            "INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine " +
            "LEFT OUTER JOIN speseTrasporto s ON  d.idDateOrdini = s.idDateOrdini AND s.idUtente = ?1 " +
            "LEFT OUTER JOIN (" +
            "  SELECT o.idDateOrdine, COUNT(*) AS friendCount, " +
            "  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted " +
            "  FROM ordini o WHERE idReferenteAmico = ?1 AND o.riepilogoUtente = 0 " +
            "  GROUP BY o.idDateOrdine " +
            ") f ON d.idDateOrdini = f.idDateOrdine " +
            "WHERE d.idDateOrdini IN ?2", nativeQuery = true)
    List<UserOrderSummary> findUserOrderSummary(String userId, Set<String> orderIds);

    @Query(value = "SELECT d.idDateOrdini AS orderId, " +
            "CASE " +
            "   WHEN t.\"external\" = 1 OR t.totaleCalcolato = 0 THEN " +
            "       (SELECT SUM(importo) FROM movimenti m WHERE d.idDateOrdini = m.idDateOrdini AND m.idUtente = ?1) " +
            "   ELSE " +
            "       (SELECT SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * " +
            "               CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END) " +
            "        FROM ordini o " +
            "        INNER JOIN prodotti p ON o.idProdotto = p.idProdotto " +
            "        WHERE d.idDateOrdini = o.idDateOrdine AND o.riepilogoUtente = (1 - t.riepilogo) AND o.idUtente = ?1" +
            "       ) " +
            "END as totalAmount, " +
            "(SELECT COUNT(*) FROM ordini o WHERE d.idDateOrdini = o.idDateOrdine AND o.riepilogoUtente = (1 - t.riepilogo) AND o.idUtente = ?1) as itemsCount, " +
            "0 as friendCount, 0 as friendAccounted, " +
            "s.importo AS shippingCost " +
            "FROM dateOrdini d " +
            "INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine " +
            "LEFT OUTER JOIN speseTrasporto s ON  d.idDateOrdini = s.idDateOrdini AND s.idUtente = ?1 " +
            "WHERE d.idDateOrdini IN ?2", nativeQuery = true)
    List<UserOrderSummary> findFriendOrderSummary(String userId, Set<String> orderIds);

    @Modifying
    @Query("UPDATE Order o SET o.statusCode = ?2 WHERE o.id = ?1")
    int updateOrderStatus(String orderId, int status);

    @Query("SELECT o FROM Order o WHERE o.statusCode = 0 AND o.openingDate <= CURRENT_TIMESTAMP AND function('DateAdd', hh, o.dueHour, o.dueDate) >= CURRENT_TIMESTAMP")
    List<Order> getOpenOrders();

    @Query("SELECT o FROM Order o JOIN FETCH o.orderType t " +
            "WHERE function('DateAdd', hh, o.dueHour, o.dueDate) < CURRENT_TIMESTAMP AND o.deliveryDate >= function('convert', date, CURRENT_TIMESTAMP) " +
            "AND (t.external = true OR EXISTS (SELECT i.id FROM OrderItem i WHERE i.user = ?1))")
    List<Order> getInDeliveryOrders(String userId);

    @Query(value = "SELECT d.idDateOrdini AS orderId, o.idUtente as userId, " +
            "CASE " +
            "   WHEN t.\"external\" = 1 OR t.totaleCalcolato = 0 THEN " +
            "       NULL " +
            "   ELSE " +
            "       SUM(o.qtaOrdinata * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END) " +
            "END as totalAmount, " +
            "COUNT(o.idProdotto) as itemsCount " +
            "FROM dateOrdini d " +
            "INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine " +
            "INNER JOIN ordini o ON d.idDateOrdini = o.idDateOrdine " +
            "INNER JOIN prodotti p ON o.idProdotto = p.idProdotto " +
            "WHERE o.riepilogoUtente = 0 " +
            "AND (o.idUtente = ?1 OR o.idReferenteAmico = ?1) " +
            "AND d.idDateOrdini IN ?2 " +
            "GROUP BY d.idDateOrdini, t.\"external\", t.totaleCalcolato, o.idUtente", nativeQuery = true)
    List<OpenOrderSummary> findOpenOrderSummary(String userId, Set<String> orderIds);

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

    @Query(value = "SELECT o.idDateOrdini as orderId, t.tipoOrdine as description, o.dataConsegna as deliveryDate, round(sum(o2.qtaRitirataKg*o2.prezzoKg),2) as total " +
        "FROM dateordini o " +
        "    inner join tipologiaOrdine t on o.idTipologiaOrdine = t.idTipologiaOrdine " +
        "    inner join ordini o2 on o.idDateOrdini = o2.idDateOrdine " +
        "WHERE o.stato = 2 " +
        "AND o.dataConsegna >= ?1 " +
        "AND o.dataConsegna < ?2 " +
        "and o2.contabilizzato=1 " +
        "group by o.idDateOrdini, t.tipoOrdine, o.dataConsegna " +
        "order by o.dataConsegna ", nativeQuery = true)
    List<OrderTotal> getOrderTotals(LocalDate deliveryDateFrom, LocalDate deliveryDateTo);
}
