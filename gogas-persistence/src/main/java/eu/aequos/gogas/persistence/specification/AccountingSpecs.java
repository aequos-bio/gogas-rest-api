package eu.aequos.gogas.persistence.specification;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.User;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Set;

public class AccountingSpecs {

    private AccountingSpecs() {}

    public static Specification<AccountingEntry> user(String userId) {
        return (entry, cq, cb) -> buildUserOrFriendPredicate(userId, entry, cb);
    }

    public static Specification<AccountingEntry> user(String userId, boolean dateAscending) {
        return (entry, cq, cb) -> {
            //setting order by
            if (dateAscending)
                cq.orderBy(cb.asc(entry.get("date")));
            else
                cq.orderBy(cb.desc(entry.get("date")));

            return buildUserOrFriendPredicate(userId, entry, cb);
        };
    }

    private static Predicate buildUserOrFriendPredicate(String userId, Root<AccountingEntry> entry, CriteriaBuilder cb) {
        return cb.or(cb.equal(entry.join("user").get("id"), userId), cb.equal(entry.get("friendReferralId"), userId));
    }

    public static Specification<AccountingEntry> users(Set<String> userIds) {
        return (entry, cq, cb) -> entry.join("user").get("id").in(userIds);
    }

    public static Specification<AccountingEntry> userRole(User.Role role) {
        return (entry, cq, cb) -> cb.equal(entry.join("user").get("role"), role.name());
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
