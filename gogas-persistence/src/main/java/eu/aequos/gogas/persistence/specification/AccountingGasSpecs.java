package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AccountingGasSpecs {

    public static Specification<AccountingGasEntry> fromDate(LocalDate fromDate) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get("date"), fromDate);
    }

    public static Specification<AccountingGasEntry> toDate(LocalDate toDate) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get("date"), toDate);
    }

    public static Specification<AccountingGasEntry> descriptionLike(String description) {
        return (entry, cq, cb) -> cb.like(entry.get("description"), "%" + description +"%");
    }

    public static Specification<AccountingGasEntry> reason(String reasonCode) {
        return (entry, cq, cb) -> cb.equal(entry.join("reason").get("reasonCode"), reasonCode);
    }

    public static Specification<AccountingGasEntry> select() {
        return (entry, cq, cb) -> {
            //forcing fetching (inline join instead of n+1 queries)
            entry.fetch("reason");

            //setting order by
            cq.orderBy(cb.desc(entry.get("date")));

            return cb.conjunction();
        };
    }
}
