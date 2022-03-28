package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OrderSpecs {

    public enum SortingType {
        NONE,
        DUE_DATE("dueDate", "dueHour"),
        DELIVERY_DATE("deliveryDate", "dueDate", "dueHour");

        SortingType(String... fields) {
            this.fields = fields;
        }

        private final String[] fields;

        public String[] getFields() {
            return fields;
        }
    }

    public static Specification<Order> dueDateFrom(LocalDate from) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get("dueDate"), from);
    }

    public static Specification<Order> dueDateTo(LocalDate to) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get("dueDate"), to);
    }

    public static Specification<Order> dueHourFrom(int from) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get("dueHour"), from);
    }

    public static Specification<Order> deliveryDateFrom(LocalDate from) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get("deliveryDate"), from);
    }

    public static Specification<Order> deliveryDateTo(LocalDate to) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get("deliveryDate"), to);
    }

    public static Specification<Order> type(String orderType) {
        return (entry, cq, cb) -> cb.equal(entry.get("orderType").get("id"), orderType);
    }

    public static Specification<Order> orderedByUser(String user) {
        return (entry, cq, cb) -> {
            Subquery<OrderItem> userOrderItemExists = cq.subquery(OrderItem.class);
            Root<OrderItem> orderItemRoot = userOrderItemExists.from(OrderItem.class);

            userOrderItemExists.select(orderItemRoot)
                    .where(cb.and(
                            cb.equal(orderItemRoot.get("order"), entry.get("id")),
                            cb.equal(orderItemRoot.get("user"), user))
                    );

            return cb.or(
                    cb.equal(entry.get("orderType").get("external"), true),
                    cb.exists(userOrderItemExists)
            );
        };
    }

    public static Specification<Order> managedByUser(List<String> managedOrderTypes) {
        return (entry, cq, cb) -> entry.get("orderType").get("id").in(managedOrderTypes);
    }

    public static Specification<Order> statusIn(List<Integer> statusCodeList) {
        return (entry, cq, cb) -> entry.get("statusCode").in(statusCodeList);
    }

    public static Specification<Order> paid(Boolean paid) {
        return (entry, cq, cb) -> cb.equal(entry.get("paid"), paid);
    }

    public static Specification<Order> select(SortingType sortingType) {
        return (entry, cq, cb) -> {
            //forcing fetching (inline join instead of n+1 queries) but only for "data" query
            if (cq.getResultType().equals(Order.class)) {
                entry.fetch("orderType");
            }

            //ordering
            applyOrderBy(sortingType, entry, cq, cb);

            //no base filter
            return cb.conjunction();
        };
    }

    private static void applyOrderBy(SortingType sortingType, Root<Order> entry, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        List<javax.persistence.criteria.Order> orderBy = Arrays.stream(sortingType.getFields())
                .map(field -> cb.desc(entry.get(field)))
                .collect(Collectors.toList());

        orderBy.add(cb.asc(entry.get("orderType").get("description")));

        cq.orderBy(orderBy);
    }
}
