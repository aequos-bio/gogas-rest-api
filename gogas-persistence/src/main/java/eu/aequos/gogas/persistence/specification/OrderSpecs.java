package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;
import java.util.List;

public class OrderSpecs {

    public static Specification<Order> dueDateFrom(Date from) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get( "dueDate"), from);
    }

    public static Specification<Order> dueDateTo(Date to) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get( "dueDate"), to);
    }

    public static Specification<Order> dueHourFrom(int from) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get( "dueHour"), from);
    }

    public static Specification<Order> deliveryDateFrom(Date from) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get( "deliveryDate"), from);
    }

    public static Specification<Order> deliveryDateTo(Date to) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get( "deliveryDate"), to);
    }

    public static Specification<Order> type(String orderType) {
        return (entry, cq, cb) -> cb.equal(entry.get( "orderType").get("id"), orderType);
    }

    public static Specification<Order> managedByUser(List<String> managedOrderTypes) {
        return (entry, cq, cb) -> entry.get( "orderType").get("id").in(managedOrderTypes);
    }

    public static Specification<Order> statusIn(List<Integer> statusCodeList) {
        return (entry, cq, cb) -> entry.get( "statusCode").in(statusCodeList);
    }

    public static Specification<Order> paid(Boolean paid) {
        return (entry, cq, cb) -> cb.equal(entry.get( "paid"), paid);
    }

    public static Specification<Order> select() {
        return (entry, cq, cb) -> {
            //forcing fetching (inline join instead of n+1 queries)
            entry.fetch("orderType");

            //ordering
            cq.orderBy(cb.desc(entry.get("dueDate")), cb.desc(entry.get("dueHour")));

            //no base filter
            return cb.conjunction();
        };
    }
}
