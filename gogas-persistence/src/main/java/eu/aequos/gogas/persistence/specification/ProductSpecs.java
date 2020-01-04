package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Order;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecs {

    public static Specification<Product> category(String categoryId) {
        return (entry, cq, cb) -> cb.equal(entry.get( "category").get("id"), categoryId);
    }

    public static Specification<Product> available(Boolean available) {
        return (entry, cq, cb) -> cb.equal(entry.get("available"), available);
    }

    public static Specification<Product> cancelled(Boolean cancelled) {
        return (entry, cq, cb) -> cb.equal(entry.get("cancelled"), cancelled);
    }

    public static Specification<Product> type(String type, boolean orderByPriceList) {
        return (entry, cq, cb) -> {
            //forcing fetching (inline join instead of n+1 queries)
            entry.fetch("category");
            entry.fetch("supplier");

            if (orderByPriceList) {
                List<Order> orderList = new ArrayList();
                orderList.add(cb.asc(entry.get("category").get("priceListPosition")));
                orderList.add(cb.asc(entry.get("description")));
                cq.orderBy(orderList);
            }

            return cb.equal(entry.get("type"), type);
        };
    }
}
