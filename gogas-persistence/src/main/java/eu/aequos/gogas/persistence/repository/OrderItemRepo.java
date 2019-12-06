package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.derived.ByUserOrderItem;
import eu.aequos.gogas.persistence.entity.derived.FriendTotalOrder;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrderItemRepo extends CrudRepository<OrderItem, String> {

    <T> List<T> findByUserAndOrderAndSummary(String userId, String orderId, boolean summary, Class<T> type);

    <T> List<T> findByProductAndOrderAndSummary(String productId, String orderId, boolean summary, Class<T> type);

    List<OrderItem> findByOrderAndSummary(String orderId, boolean summary);

    <T> Optional<T> findByUserAndOrderAndProductAndSummary(String userId, String orderId, String product, boolean summary, Class<T> type);

    @Query("SELECT o.id FROM OrderItem o WHERE o.id = ?1 AND (o.user = ?2 OR o.friendReferral = ?2)")
    Optional<String> findOrderItemByIdAndUserOrFriend(String orderItemId, String userId);

    @Query("SELECT o FROM OrderItem o WHERE (o.user = ?1 OR o.friendReferral = ?1) AND o.order = ?2 AND o.product = ?3 AND o.summary = false")
    <T> List<T> findNotSummaryByUserOrReferral(String userId, String orderId, String product, Class<T> type);

    @Query("SELECT o.product AS product, SUM(o.deliveredQuantity) AS totalQuantity, COUNT(DISTINCT o.user) AS userCount, " +
            "CASE WHEN SUM(1 - o.accounted) = 0 THEN true ELSE false END AS accounted " +
            "FROM OrderItem o WHERE (o.user = ?1 OR o.friendReferral = ?1) AND o.order = ?2 AND o.summary = false " +
            "GROUP BY o.product")
    List<FriendTotalOrder> totalQuantityNotSummaryByUserOrReferral(String userId, String orderId);

    @Query("SELECT o.product AS product, SUM(o.deliveredQuantity) AS totalQuantity, COUNT(DISTINCT o.user) AS userCount, " +
            "CASE WHEN SUM(1 - o.cancelled) = 0 THEN true ELSE false END AS cancelled " +
            "FROM OrderItem o " +
            "WHERE o.order = ?1 and o.summary = true " +
            "GROUP BY o.product")
    List<ProductTotalOrder> totalQuantityAndUsersByProductForClosedOrder(String orderId);

    @Query(value = "SELECT o.idProdotto as product, " +
            "SUM(CASE WHEN o.um = p.umCollo THEN o.qtaOrdinata * p.pesoCassa ELSE o.qtaOrdinata END) as totalQuantity, " +
            "COUNT(DISTINCT o.idUtente) as userCount, " +
            "CAST(0 as bit) AS cancelled " +
            "FROM ordini o " +
            "INNER JOIN prodotti p ON o.idProdotto = p.idProdotto " +
            "WHERE o.idDateOrdine = ?1 and o.riepilogoUtente = 0 " +
            "GROUP BY o.idProdotto", nativeQuery = true)
    List<ProductTotalOrder> totalQuantityAndUsersByProductForOpenOrder(String orderId);

    @Query(value = "SELECT o.idProdotto as product, " +
            "SUM(CASE WHEN o.um = p.umCollo THEN o.qtaOrdinata * p.pesoCassa ELSE o.qtaOrdinata END) as totalQuantity, " +
            "COUNT(DISTINCT o.idUtente) as userCount, " +
            "CAST(0 as bit) AS cancelled " +
            "FROM ordini o " +
            "INNER JOIN prodotti p ON o.idProdotto = p.idProdotto " +
            "WHERE o.idDateOrdine = ?1 and o.riepilogoUtente = 0 AND o.idProdotto = ?2 " +
            "GROUP BY o.idProdotto", nativeQuery = true)
    Optional<ProductTotalOrder> totalQuantityAndUsersByProductForOpenOrder(String orderId, String productId);

    long countDistinctUserByOrder(String orderId);

    @Query("SELECT o.user AS userId, COUNT(o.product) AS orderedItems, SUM(o.deliveredQuantity * o.price) AS totalAmount " +
            "FROM OrderItem o " +
            "WHERE o.order = ?1 and o.summary = true " +
            "GROUP BY o.user")
    List<ByUserOrderItem> itemsCountAndAmountByUserForClosedOrder(String orderId);

    @Modifying
    int deleteByOrderAndSummary(String orderId, boolean summary);

    @Modifying
    @Query("UPDATE OrderItem o SET o.accounted = ?2 WHERE o.summary = true AND order = ?1")
    int setAccountedByOrderId(String orderId, boolean accounted);

    @Modifying
    @Query("UPDATE OrderItem o SET o.cancelled = ?2 WHERE o.summary = false AND order = ?1")
    int setCancelledByOrderId(String orderId, boolean cancelled);

    @Transactional
    @Modifying
    @Query("UPDATE OrderItem o SET o.cancelled = true, o.deliveredQuantity = 0 WHERE o.summary = true AND order = ?1 AND product = ?2")
    int cancelByOrderAndProduct(String orderId, String productId);

    @Transactional
    @Modifying
    @Query("UPDATE OrderItem o SET o.cancelled = false, o.deliveredQuantity = o.orderedQuantity WHERE o.summary = true AND order = ?1 AND product = ?2")
    int restoreByOrderAndProduct(String orderId, String productId);

    @Transactional
    @Modifying
    @Query("UPDATE OrderItem o SET o.cancelled = true, o.deliveredQuantity = 0 WHERE o.id = ?1")
    int cancelByOrderItem(String orderItemId);

    @Transactional
    @Modifying
    @Query("UPDATE OrderItem o SET o.cancelled = false, o.deliveredQuantity = o.orderedQuantity WHERE o.id = ?1")
    int restoreByOrderItem(String orderItemId);

    @Modifying
    @Query("UPDATE OrderItem o SET o.deliveredQuantity = o.deliveredQuantity + ?4 WHERE o.user = ?1 AND o.order = ?2 AND o.product = ?3 AND summary = true")
    int addDeliveredQtyToOrderItem(String userId, String orderId, String product, BigDecimal deliveredQty);

    @Transactional
    @Modifying
    @Query("UPDATE OrderItem o SET o.deliveredQuantity = o.deliveredQuantity + (o.deliveredQuantity * ?3) WHERE o.order = ?1 AND o.product = ?2 AND summary = true")
    int increaseDeliveredQtyByProduct(String orderId, String product, BigDecimal deliveredQtyRatio);

    @Transactional
    @Modifying
    @Query("UPDATE OrderItem o SET o.deliveredQuantity = ?3 WHERE o.order = ?1 AND o.id = ?2")
    int updateDeliveredQtyByItemId(String orderId, String orderItemId, BigDecimal deliveredQty);

    @Modifying
    @Query("UPDATE OrderItem o SET o.price = ?3 WHERE o.order = ?1 AND o.product = ?2")
    int updatePriceByOrderIdAndProductId(String orderId, String productId, BigDecimal price);

    @Transactional
    @Modifying
    @Query("UPDATE OrderItem o SET o.accounted = ?4 WHERE o.order = ?2 AND o.product = ?3 AND (o.user = ?1 OR o.friendReferral = ?1) AND o.summary = false")
    int updateAccountedByOrderIdAndProductIdAndUserOrFriend(String userId, String orderId, String productId, boolean accounted);


    @Query("SELECT DISTINCT o.user FROM OrderItem o WHERE o.order = ?1 AND o.product = ?2 and o.summary = ?3")
    Set<String> findUserOrderingByProductAndSummary(String orderId, String productId, boolean summary);
}
