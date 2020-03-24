package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceEntryDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.persistence.specification.AccountingSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.persistence.specification.UserBalanceSpecs;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.aequos.gogas.persistence.entity.AccountingEntryReason.*;

@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private AccountingRepo accountingRepo;
    private AccountingReasonRepo accountingReasonRepo;
    private UserBalanceEntryRepo userBalanceEntryRepo;
    private UserService userService;
    private UserRepo userRepo;
    private YearRepo yearRepo;

    public AccountingService(AccountingRepo accountingRepo, AccountingReasonRepo accountingReasonRepo,
                             UserBalanceEntryRepo userBalanceEntryRepo, UserService userService,
                             UserRepo userRepo, YearRepo yearRepo) {

        super(accountingRepo, "accounting entry");
        this.accountingRepo = accountingRepo;
        this.accountingReasonRepo = accountingReasonRepo;
        this.userBalanceEntryRepo = userBalanceEntryRepo;
        this.userService = userService;
        this.userRepo = userRepo;
        this.yearRepo = yearRepo;
    }

    public BigDecimal getBalance(String userId) {
        return userRepo.getBalance(userId);
    }

    public AccountingEntryDTO get(String entryId) {
        AccountingEntry existingEntry = getRequired(entryId);
        return new AccountingEntryDTO().fromModel2(existingEntry);
    }

    @Transactional
    public AccountingEntry create(AccountingEntryDTO dto) throws GoGasException {
        if (isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere creato, l'anno contabile è chiuso");

        AccountingEntry createdEntry = super.create(dto);
        userRepo.updateBalance(createdEntry.getUser().getId(), getSignedAmount(createdEntry));

        return createdEntry;
    }

    @Transactional
    public AccountingEntry update(String entryId, AccountingEntryDTO dto) throws ItemNotFoundException, GoGasException {
        AccountingEntry existingEntry = getRequired(entryId);
        BigDecimal previousAmount = getSignedAmount(existingEntry);

        if (isYearClosed(existingEntry) || isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere modificato, l'anno contabile è chiuso");

        AccountingEntry updatedEntry = super.createOrUpdate(existingEntry, dto);
        BigDecimal amountDifference = getSignedAmount(updatedEntry).subtract(previousAmount);
        userRepo.updateBalance(updatedEntry.getUser().getId(), amountDifference);

        return super.createOrUpdate(existingEntry, dto);
    }

    @Transactional
    public void delete(String entryId) {
        AccountingEntry entry = getRequired(entryId);
        userRepo.updateBalance(entry.getUser().getId(), getSignedAmount(entry).negate());
        super.delete(entryId);
    }

    private BigDecimal getSignedAmount(AccountingEntry entry) {
        Sign sign = accountingReasonRepo.findById(entry.getReason().getReasonCode())
                .map(AccountingEntryReason::getSignEnum)
                .get();

        return sign.getSignedAmount(entry.getAmount());
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
        return toUserBalanceDTO(userRepo.findByRole(User.Role.U.name()));
    }

    public List<UserBalanceDTO> getFriendBalanceList(String referralId) {
        return toUserBalanceDTO(userRepo.findByFriendReferralId(referralId, User.class));
    }

    private List<UserBalanceDTO> toUserBalanceDTO(List<User> userList) {
        return userList.stream()
                .map(balance -> new UserBalanceDTO().fromModel(balance, userService.getUserDisplayName(balance.getFirstName(), balance.getLastName())))
                .sorted(Comparator.comparing(UserBalanceDTO::getFullName))
                .collect(Collectors.toList());
    }

    public UserBalanceSummaryDTO getUserBalance(String userId, LocalDate dateFrom, LocalDate dateTo, boolean dateAscending) {
        Specification<UserBalanceEntry> filter = new SpecificationBuilder<UserBalanceEntry>()
                .withBaseFilter(UserBalanceSpecs.user(userId, dateAscending))
                .and(UserBalanceSpecs::fromDate, dateFrom)
                .and(UserBalanceSpecs::toDate, dateTo)
                .build();

        List<UserBalanceEntryDTO> entries = userBalanceEntryRepo.findAll(filter).stream()
                .map(entry -> new UserBalanceEntryDTO().fromModel(entry))
                .collect(Collectors.toList());

        BigDecimal balance = userRepo.getBalance(userId);

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

    @Transactional
    public int setEntriesConfirmedByOrderId(String orderId, boolean confirmed) {
        int updatedEntries = accountingRepo.setConfirmedByOrderId(orderId, confirmed);
        userRepo.updateBalancesFromAccountingEntriesByOrderId(orderId);
        return updatedEntries;
    }

    public long countEntriesByOrderId(String orderId) {
        return accountingRepo.countByOrderId(orderId);
    }

    public void updateBalancesFromOrderItemsByOrderId(String orderId) {
        userRepo.updateBalancesFromOrderItemsByOrderId(orderId);
    }

    public int updateFriendBalancesFromOrderItems(String referralId, String orderId, String productId) {
        return userRepo.updateFriendBalancesFromOrderItems(referralId, orderId, productId);
    }
}
