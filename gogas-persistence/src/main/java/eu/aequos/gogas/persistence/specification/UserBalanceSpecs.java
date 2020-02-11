package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.UserBalanceEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class UserBalanceSpecs {

    public static Specification<UserBalanceEntry> fromDate(LocalDate fromDate) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get("date"), fromDate);
    }

    public static Specification<UserBalanceEntry> toDate(LocalDate toDate) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get("date"), toDate);
    }

    public static Specification<UserBalanceEntry> user(String userId, boolean dateAscending) {
        return (entry, cq, cb) -> {
            //setting order by
            if (dateAscending)
                cq.orderBy(cb.asc(entry.get("date")));
            else
                cq.orderBy(cb.desc(entry.get("date")));

            return cb.equal(entry.get("userId"), userId);
        };
    }
}
