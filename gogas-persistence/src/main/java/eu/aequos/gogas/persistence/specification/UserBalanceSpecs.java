package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.UserBalanceEntry;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

public class UserBalanceSpecs {

    public static Specification<UserBalanceEntry> fromDate(Date fromDate) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get("date"), fromDate);
    }

    public static Specification<UserBalanceEntry> toDate(Date toDate) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get("date"), toDate);
    }

    public static Specification<UserBalanceEntry> user(String userId) {
        return (entry, cq, cb) -> {
            //setting order by
            cq.orderBy(cb.desc(entry.get("date")));

            return cb.equal(entry.get("userId"), userId);
        };
    }
}
