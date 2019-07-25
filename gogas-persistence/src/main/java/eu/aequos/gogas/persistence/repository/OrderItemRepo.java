package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderItemRepo extends CrudRepository<OrderItem, String> {

    <T> List<T> findByUserAndOrderAndSummary(String userId, String orderId, boolean summary, Class<T> type);

    List<OrderItem> findByOrderAndSummary(String orderId, boolean summary);

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
}
