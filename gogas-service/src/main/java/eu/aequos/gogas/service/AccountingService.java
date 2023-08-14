package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceEntryDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.AuditUserBalance.EntryType;
import eu.aequos.gogas.persistence.entity.AuditUserBalance.OperationType;
import eu.aequos.gogas.persistence.entity.derived.OrderUserTotal;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.persistence.specification.AccountingSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.persistence.specification.UserBalanceSpecs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static eu.aequos.gogas.converter.ListConverter.toMap;
import static eu.aequos.gogas.persistence.entity.AccountingEntryReason.Sign;

@Slf4j
@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private final AccountingRepo accountingRepo;
    private final AccountingReasonRepo accountingReasonRepo;
    private final UserBalanceEntryRepo userBalanceEntryRepo;
    private final UserService userService;
    private final UserRepo userRepo;
    private final YearRepo yearRepo;
    private final OrderRepo orderRepo;
    private final AuditUserBalanceRepo auditUserBalanceRepo;

    public AccountingService(AccountingRepo accountingRepo, AccountingReasonRepo accountingReasonRepo,
                             UserBalanceEntryRepo userBalanceEntryRepo, UserService userService, UserRepo userRepo,
                             YearRepo yearRepo, OrderRepo orderRepo, AuditUserBalanceRepo auditUserBalanceRepo) {

        super(accountingRepo, "accounting entry");
        this.accountingRepo = accountingRepo;
        this.accountingReasonRepo = accountingReasonRepo;
        this.userBalanceEntryRepo = userBalanceEntryRepo;
        this.userService = userService;
        this.userRepo = userRepo;
        this.yearRepo = yearRepo;
        this.orderRepo = orderRepo;
        this.auditUserBalanceRepo = auditUserBalanceRepo;
    }

    public BigDecimal getBalance(String userId) {
        return userRepo.getBalance(userId);
    }

    @Transactional
    public AccountingEntry create(AccountingEntryDTO dto) throws GoGasException {
        if (isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere creato, l'anno contabile è chiuso");

        AccountingEntry createdEntry = super.create(dto);
        log.info("New accounting entry created for user {} (id: {})", createdEntry.getUser().getId(), createdEntry.getId());

        String userId = createdEntry.getUser().getId();
        BigDecimal signedAmount = getSignedAmount(createdEntry);
        updateBalance(userId, signedAmount, createdEntry.getId(), EntryType.ACCOUNTING, OperationType.ADD);

        return createdEntry;
    }

    public AccountingEntryDTO get(String entryId) {
        AccountingEntry existingEntry = getRequired(entryId);
        return new AccountingEntryDTO().fromModel(existingEntry);
    }

    @Transactional
    public AccountingEntry update(String entryId, AccountingEntryDTO dto) throws ItemNotFoundException, GoGasException {
        AccountingEntry existingEntry = getRequired(entryId);
        BigDecimal previousAmount = getSignedAmount(existingEntry);

        if (existingEntry.getOrderId() != null)
            throw new GoGasException("Movimento relativo ad un ordine, non può essere modificato");

        if (isYearClosed(existingEntry) || isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere modificato, l'anno contabile è chiuso");

        if (!existingEntry.getUser().getId().equalsIgnoreCase(dto.getUserId())) {
            throw new GoGasException("Non è possibile modificare l'utente relativo al movimento");
        }

        AccountingEntry updatedEntry = super.createOrUpdate(existingEntry, dto);
        log.info("Accounting entry updated for user {} (id: {})", updatedEntry.getUser().getId(), updateEntry.getId());

        BigDecimal amountDifference = getSignedAmount(updatedEntry).subtract(previousAmount);
        String userId = updatedEntry.getUser().getId();
        updateBalance(userId, amountDifference, updatedEntry.getId(), EntryType.ACCOUNTING, OperationType.UPDATE);

        return updatedEntry;
    }

    @Override
    @Transactional
    public void delete(String entryId) {
        AccountingEntry existingEntry = getRequired(entryId);

        if (existingEntry.getOrderId() != null)
            throw new GoGasException("Movimento relativo ad un ordine, non può essere eliminato");

        if (isYearClosed(existingEntry))
            throw new GoGasException("Il movimento non può essere eliminato, l'anno contabile è chiuso");

        BigDecimal amount = getSignedAmount(existingEntry).negate();
        String userId = existingEntry.getUser().getId();

        updateBalance(userId, amount, entryId, EntryType.ACCOUNTING, OperationType.REMOVE);

        super.delete(entryId);
    }

    private BigDecimal getSignedAmount(AccountingEntry entry) {
        String reasonCode = entry.getReason().getReasonCode();

        Sign sign = accountingReasonRepo.findById(reasonCode)
                .map(AccountingEntryReason::getSignEnum)
                .orElseThrow(() -> new GoGasException("Invalid accounting reason " + reasonCode));

        return sign.getSignedAmount(entry.getAmount());
    }

    public List<AccountingEntryDTO> getUserAccountingEntries(String userId, String reasonCode,
                                                             String description, LocalDate dateFrom,
                                                             LocalDate dateTo) {

        Set<String> userIds = Optional.ofNullable(userId)
                .map(Set::of)
                .orElse(null);

        return getAccountingEntries(userIds, User.Role.U, reasonCode, description, dateFrom, dateTo);
    }

    public List<AccountingEntryDTO> getFriendsAccountingEntries(String selectedFriendId, String reasonCode,
                                                                String description, LocalDate dateFrom,
                                                                LocalDate dateTo, String userId) {

        Set<String> friendsIds = buildFriendListForFilter(selectedFriendId, userId);

        if (friendsIds.isEmpty()) {
            return Collections.emptyList();
        }

        return getAccountingEntries(friendsIds, User.Role.S, reasonCode, description, dateFrom, dateTo);
    }

    private Set<String> buildFriendListForFilter(String selectedFriendId, String userId) {
        Set<String> allFriendsIds = userService.getFriendsIds(userId);

        if (selectedFriendId == null) {
            return allFriendsIds;
        }

        if (!allFriendsIds.contains(selectedFriendId)) {
            return Collections.emptySet();
        }

        return Set.of(selectedFriendId);
    }

    private List<AccountingEntryDTO> getAccountingEntries(Set<String> userIds, User.Role userRole,
                                                          String reasonCode, String description,
                                                          LocalDate dateFrom, LocalDate dateTo) {

        Specification<AccountingEntry> filter = new SpecificationBuilder<AccountingEntry>()
                .withBaseFilter(AccountingSpecs.notLinkedToOrder())
                .and(AccountingSpecs::users, userIds)
                .and(AccountingSpecs::userRole, userRole)
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

    public UserBalanceSummaryDTO getUserBalance(String userId, LocalDate dateFrom, LocalDate dateTo,
                                                boolean dateAscending) {
        return getPaginatedUserBalance(userId, dateFrom, dateTo, dateAscending, null, null);
    }

    public UserBalanceSummaryDTO getPaginatedUserBalance(String userId, LocalDate dateFrom, LocalDate dateTo,
                                                boolean dateAscending, Integer skipItems, Integer maxItems) {

        Specification<UserBalanceEntry> filter = new SpecificationBuilder<UserBalanceEntry>()
                .withBaseFilter(UserBalanceSpecs.user(userId, dateAscending))
                .and(UserBalanceSpecs::fromDate, dateFrom)
                .and(UserBalanceSpecs::toDate, dateTo)
                .build();

        List<UserBalanceEntry> entries;
        if (skipItems != null) {
            entries = userBalanceEntryRepo.findAll(filter, PageRequest.of(skipItems / maxItems, maxItems)).getContent();
        } else {
            entries = userBalanceEntryRepo.findAll(filter);
        }

        Set<String> orderIds = entries.stream()
                .map(UserBalanceEntry::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Order> relatedOrders = getRelatedOrders(orderIds);

        List<UserBalanceEntryDTO> dtoEntries = entries.stream()
                .map(entry -> new UserBalanceEntryDTO().fromModel(entry, relatedOrders))
                .collect(Collectors.toList());

        BigDecimal balance = userRepo.getBalance(userId);

        return new UserBalanceSummaryDTO(balance, dtoEntries);
    }

    private Map<String, Order> getRelatedOrders(Set<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return orderRepo.findByIdIn(orderIds).stream()
                .collect(toMap(Order::getId));
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
    public int setEntriesConfirmedByOrderId(String orderId, boolean charge) {
        int updatedEntries = accountingRepo.setConfirmedByOrderId(orderId, charge);

        List<OrderUserTotal> orderUserTotals = accountingRepo.getOrderTotals(orderId);
        updateUserBalances(orderId, orderUserTotals, charge);

        return updatedEntries;
    }

    public long countEntriesByOrderId(String orderId) {
        return accountingRepo.countByOrderId(orderId);
    }

    @Transactional
    public void updateBalancesFromOrderItemsByOrderId(String orderId, boolean charge) {
        List<OrderUserTotal> orderUserTotals = orderRepo.getComputedOrderTotalsForAccounting(orderId);
        updateUserBalances(orderId, orderUserTotals, charge);
    }

    @Transactional
    public void updateFriendBalancesFromOrderItems(String referralId, String orderId, String productId, boolean charge) {
        List<OrderUserTotal> orderUserTotals = orderRepo.getComputedOrderTotalsForFriendAccounting(referralId, orderId, productId);
        updateUserBalances(orderId, orderUserTotals, charge);
    }

    private void updateUserBalances(String orderId, List<OrderUserTotal> orderUserTotals, boolean charge) {
        BinaryOperator<BigDecimal> reduceOperator = charge ? BigDecimal::subtract : BigDecimal::add;
        OperationType operationType = charge ? OperationType.ADD : OperationType.REMOVE;

        for (OrderUserTotal orderUserTotal : orderUserTotals) {
            BigDecimal cumulativeAmount = orderUserTotals.stream()
                    .filter(nestedOrderUserTotal -> nestedOrderUserTotal.isUserOrFriend(orderUserTotal.getUserId()))
                    .map(OrderUserTotal::getTotalAmount)
                    .reduce(BigDecimal.ZERO, reduceOperator);

            updateBalance(orderUserTotal.getUserId(), cumulativeAmount, orderId, EntryType.ORDER, operationType);
        }
    }

    private synchronized void updateBalance(String userId, BigDecimal amount, String entryId, EntryType entryType,
                                            OperationType operationType) {

        BigDecimal currentBalance = userRepo.getBalance(userId);

        userRepo.updateBalance(userId, amount);

        AuditUserBalance auditUserBalance = new AuditUserBalance();
        auditUserBalance.setUserId(userId);
        auditUserBalance.setTs(LocalDateTime.now());
        auditUserBalance.setEntryType(entryType);
        auditUserBalance.setOperationType(operationType);
        auditUserBalance.setReferenceId(entryId);
        auditUserBalance.setAmount(amount);
        auditUserBalance.setCurrentBalance(currentBalance);

        auditUserBalanceRepo.save(auditUserBalance);
    }
}
