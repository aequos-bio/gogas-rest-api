package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceEntryDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.repository.UserBalanceEntryRepo;
import eu.aequos.gogas.persistence.repository.UserBalanceRepo;
import eu.aequos.gogas.persistence.specification.AccountingSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.persistence.specification.UserBalanceSpecs;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private AccountingRepo accountingRepo;
    private UserBalanceRepo userBalanceRepo;
    private UserBalanceEntryRepo userBalanceEntryRepo;
    private UserService userService;

    public AccountingService(AccountingRepo accountingRepo, UserBalanceRepo userBalanceRepo,
                             UserBalanceEntryRepo userBalanceEntryRepo, UserService userService) {

        super(accountingRepo, "accounting entry");
        this.accountingRepo = accountingRepo;
        this.userBalanceRepo = userBalanceRepo;
        this.userBalanceEntryRepo = userBalanceEntryRepo;
        this.userService = userService;
    }

    public BigDecimal getBalance(String userId) {
        return userBalanceRepo.getBalance(userId);
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

    public List<UserBalanceDTO> getUserBalanceList() {
        return toUserBalanceDTO(userBalanceRepo.findAll());
    }

    public List<UserBalanceDTO> getFriendBalanceList(String referralId) {
        return toUserBalanceDTO(userBalanceRepo.findByReferralId(referralId));
    }

    private List<UserBalanceDTO> toUserBalanceDTO(List<UserBalance> userBalanceList) {
        return userBalanceList.stream()
                .map(balance -> new UserBalanceDTO().fromModel(balance, userService.getUserDisplayName(balance.getFirstName(), balance.getLastName())))
                .sorted(Comparator.comparing(UserBalanceDTO::getFullName))
                .collect(Collectors.toList());
    }

    public UserBalanceSummaryDTO getUserBalance(String userId, Date dateFrom, Date dateTo) {
        Specification<UserBalanceEntry> filter = new SpecificationBuilder<UserBalanceEntry>()
                .withBaseFilter(UserBalanceSpecs.user(userId))
                .and(UserBalanceSpecs::fromDate, dateFrom)
                .and(UserBalanceSpecs::toDate, dateTo)
                .build();

        List<UserBalanceEntryDTO> entries = userBalanceEntryRepo.findAll(filter).stream()
                .map(entry -> new UserBalanceEntryDTO().fromModel(entry))
                .collect(Collectors.toList());

        BigDecimal balance = userBalanceRepo.getBalance(userId);

        return new UserBalanceSummaryDTO(balance, entries);
    }
}
