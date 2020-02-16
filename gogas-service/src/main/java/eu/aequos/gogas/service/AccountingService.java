package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceEntryDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.repository.UserBalanceEntryRepo;
import eu.aequos.gogas.persistence.repository.UserBalanceRepo;
import eu.aequos.gogas.persistence.repository.YearRepo;
import eu.aequos.gogas.persistence.specification.AccountingSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.persistence.specification.UserBalanceSpecs;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private AccountingRepo accountingRepo;
    private UserBalanceRepo userBalanceRepo;
    private UserBalanceEntryRepo userBalanceEntryRepo;
    private UserService userService;
    private YearRepo yearRepo;

    public AccountingService(AccountingRepo accountingRepo, UserBalanceRepo userBalanceRepo,
                             UserBalanceEntryRepo userBalanceEntryRepo, UserService userService,
                             YearRepo yearRepo) {

        super(accountingRepo, "accounting entry");
        this.accountingRepo = accountingRepo;
        this.userBalanceRepo = userBalanceRepo;
        this.userBalanceEntryRepo = userBalanceEntryRepo;
        this.userService = userService;
        this.yearRepo = yearRepo;
    }

    public BigDecimal getBalance(String userId) {
        return userBalanceRepo.getBalance(userId);
    }

    public AccountingEntry create(AccountingEntryDTO dto) throws GoGasException {
        if (isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere creato, l'anno contabile è chiuso");

        return super.create(dto);
    }

    public AccountingEntryDTO get(String entryId) {
        AccountingEntry existingEntry = getRequired(entryId);
        return new AccountingEntryDTO().fromModel2(existingEntry);
    }

    public AccountingEntry update(String entryId, AccountingEntryDTO dto) throws ItemNotFoundException, GoGasException {
        AccountingEntry existingEntry = getRequired(entryId);

        if (isYearClosed(existingEntry) || isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere modificato, l'anno contabile è chiuso");

        return super.createOrUpdate(existingEntry, dto);
    }

    public List<AccountingEntryDTO> getAccountingEntries(String userId, String reasonCode,
                                                         String description, LocalDate dateFrom,
                                                         LocalDate dateTo, String friendReferralId) {

        Specification<AccountingEntry> filter = new SpecificationBuilder<AccountingEntry>()
                .withBaseFilter(AccountingSpecs.notLinkedToOrder())
                .and(AccountingSpecs::user, userId)
                .and(AccountingSpecs::reason, reasonCode)
                .and(AccountingSpecs::descriptionLike, description)
                .and(AccountingSpecs::fromDate, dateFrom)
                .and(AccountingSpecs::toDate, dateTo)
                .and(AccountingSpecs::isFriendOf, friendReferralId)
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
        entry.setDescription("Totale ordine " + order.getOrderType().getDescription() + " in consegna " + ConfigurationService.formatDate(order.getDeliveryDate()));
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

    public UserBalanceSummaryDTO getUserBalance(String userId, LocalDate dateFrom, LocalDate dateTo) {
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

    private boolean isYearClosed(AccountingEntry accountingEntry) {
        return isYearClosed(accountingEntry.getDate());
    }

    private boolean isYearClosed(AccountingEntryDTO accountingEntryDTO) {
        return isYearClosed(accountingEntryDTO.getDate());
    }

    public boolean isYearClosed(LocalDate accountingDate) {
        int year = accountingDate.getYear();
        return yearRepo.existsYearByYearAndClosed(year, true);
    }

    public int setEntriesConfirmedByOrderId(String orderId, boolean confirmed) {
        return accountingRepo.setConfirmedByOrderId(orderId, confirmed);
    }

    public long countEntriesByOrderId(String orderId) {
        return accountingRepo.countByOrderId(orderId);
    }
}
