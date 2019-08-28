package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.specification.AccountingSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private AccountingRepo accountingRepo;

    public AccountingService(AccountingRepo accountingRepo) {
        super(accountingRepo, "accounting entry");
        this.accountingRepo = accountingRepo;
    }

    public BigDecimal getBalance(String userId) {
        return accountingRepo.getBalance(userId);
    }

    public List<AccountingEntryDTO> getAccountingEntries(String userId, String reasonCode,
                                                         String description, Date dateFrom, Date dateTo) {

        Specification<AccountingEntry> filter = new SpecificationBuilder<AccountingEntry>()
                .withBaseFilter(AccountingSpecs.notLinkedToOrder())
                .and(AccountingSpecs::user, userId)
                .and(AccountingSpecs::reason, reasonCode)
                .and(AccountingSpecs::descriptionLike, description)
                .and(AccountingSpecs::fromDate, dateFrom)
                .and(AccountingSpecs::toDate, dateTo)
                .build();

        return accountingRepo.findAll(filter).stream()
                .map(entry -> new AccountingEntryDTO().fromModel(entry))
                .collect(Collectors.toList());
    }

    public String createOrUpdateEntryForOrder(Order order, User user, BigDecimal amount) {
        AccountingEntry userOrderEntry = accountingRepo.findByOrderIdAndUserId(order.getId(), user.getId())
                .orElse(prepareAccountingEntryForOrder(order, user));

        userOrderEntry.setAmount(amount);
        return accountingRepo.save(userOrderEntry).getId();
    }

    private AccountingEntry prepareAccountingEntryForOrder(Order order, User user) {
        AccountingEntry entry = new AccountingEntry();
        entry.setUser(user);
        entry.setOrderId(order.getId());
        entry.setDate(order.getDeliveryDate());
        entry.setReason(new AccountingEntryReason().withReasonCode("ORDINE"));
        entry.setDescription("Totale ordine " + order.getOrderType().getDescription() + " in consegna " + ConfigurationService.getDateFormat().format(order.getDeliveryDate()));
        entry.setConfirmed(false);

        if (user.getRoleEnum().isFriend() && user.getFriendReferral() != null) {
            entry.setFriendReferralId(user.getFriendReferral().getId());
        }

        return entry;
    }

    public Set<String> getUsersWithOrder(String orderId) {
        return accountingRepo.findByOrderId(orderId).stream()
                .map(entry -> entry.getUser().getId())
                .collect(Collectors.toSet());
    }

    public boolean deleteUserEntryForOrder(String orderId, String userId) {
        return accountingRepo.deleteByOrderIdAndUserId(orderId, userId) > 0;
    }

    public List<AccountingEntry> getOrderAccountingEntries(String orderId) {
        return accountingRepo.findByOrderId(orderId);
    }
}
