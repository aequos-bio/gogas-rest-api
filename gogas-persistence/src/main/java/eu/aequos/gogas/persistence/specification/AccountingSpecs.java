package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AccountingSpecs {

    public static Specification<AccountingEntry> user(String userId) {
        return (entry, cq, cb) -> cb.equal(entry.join("user").get("id"), userId);
    }

    public static Specification<AccountingEntry> fromDate(LocalDate fromDate) {
        return (entry, cq, cb) -> cb.greaterThanOrEqualTo(entry.get("date"), fromDate);
    }

    public static Specification<AccountingEntry> toDate(LocalDate toDate) {
        return (entry, cq, cb) -> cb.lessThanOrEqualTo(entry.get("date"), toDate);
    }

    public static Specification<AccountingEntry> descriptionLike(String description) {
        return (entry, cq, cb) -> cb.like(entry.get("description"), "%" + description +"%");
    }

    public static Specification<AccountingEntry> isFriendOf(String userId) {
        return (entry, cq, cb) -> cb.like(entry.get("friendReferralId"), "%" + userId +"%");
    }

    public static Specification<AccountingEntry> reason(String reasonCode) {
        return (entry, cq, cb) -> cb.equal(entry.join("reason").get("reasonCode"), reasonCode);
    }

    public static Specification<AccountingEntry> notLinkedToOrder() {
        return (entry, cq, cb) -> {
            //forcing fetching (inline join instead of n+1 queries)
            entry.fetch("user");
            entry.fetch("reason");

            //setting order by
            cq.orderBy(cb.desc(entry.get("date")));

            return cb.and(
                    cb.isNull(entry.get("orderId")),
                    cb.isTrue(entry.get("confirmed"))
            );
        };
    }
}
