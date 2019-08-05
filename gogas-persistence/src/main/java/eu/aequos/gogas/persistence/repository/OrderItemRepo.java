package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepo extends CrudRepository<OrderItem, String> {

    <T> List<T> findByUserAndOrderAndSummary(String userId, String orderId, boolean summary, Class<T> type);

    List<OrderItem> findByOrderAndSummary(String orderId, boolean summary);

    Optional<OrderItem> findByUserAndOrderAndProductAndSummary(String userId, String orderId, String product, boolean summary);

    @Query("SELECT SUM(o.orderedQuantity), COUNT(DISTINCT o.user) FROM OrderItem o WHERE o.order = ?1 and o.summary = true")
    List<ProductTotalOrder> totalQuantityAndUsersByProductForClosedOrder(String orderId);

    @Query(value = "SELECT o.idProdotto as product, " +
            "SUM(CASE WHEN o.um = p.umCollo THEN o.qtaOrdinata * p.pesoCassa ELSE o.qtaOrdinata END) as totalQuantity, " +
            "COUNT(DISTINCT o.idUtente) as userCount " +
            "FROM ordini o " +
            "INNER JOIN prodotti p ON o.idProdotto = p.idProdotto " +
            "WHERE o.idDateOrdine = ?1 and o.riepilogoUtente = 0 " +
            "GROUP BY o.idProdotto", nativeQuery = true)
    List<ProductTotalOrder> totalQuantityAndUsersByProductForOpenOrder(String orderId);

    @Query(value = "SELECT o.idProdotto as product, " +
            "SUM(CASE WHEN o.um = p.umCollo THEN o.qtaOrdinata * p.pesoCassa ELSE o.qtaOrdinata END) as totalQuantity, " +
            "COUNT(DISTINCT o.idUtente) as userCount " +
            "FROM ordini o " +
            "INNER JOIN prodotti p ON o.idProdotto = p.idProdotto " +
            "WHERE o.idDateOrdine = ?1 and o.riepilogoUtente = 0 AND o.idProdotto = ?2 " +
            "GROUP BY o.idProdotto", nativeQuery = true)
    Optional<ProductTotalOrder> totalQuantityAndUsersByProductForOpenOrder(String orderId, String productId);

    long countDistinctUserByOrder(String orderId);

    @Modifying
    int deleteByOrderAndSummary(String orderId, boolean summary);

    @Modifying
    @Query("UPDATE OrderItem o SET o.accounted = ?2 WHERE o.summary = true AND order = ?1")
    int setAccountedByOrderId(String orderId, boolean accounted);

    @Modifying
    @Query("UPDATE OrderItem o SET o.cancelled = ?2 WHERE o.summary = false AND order = ?1")
    int setCancelledByOrderId(String orderId, boolean cancelled);
}
