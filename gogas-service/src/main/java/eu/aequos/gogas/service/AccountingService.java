package eu.aequos.gogas.service;

import com.google.common.collect.Iterables;
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
import eu.aequos.gogas.persistence.utils.UserTransactionFull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static eu.aequos.gogas.converter.ListConverter.toMap;
import static eu.aequos.gogas.persistence.entity.AccountingEntryReason.Sign;

@RequiredArgsConstructor
@Slf4j
@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private final AccountingRepo accountingRepo;
    private final AccountingReasonRepo accountingReasonRepo;
    private final ShippingCostRepo shippingCostRepo;
    private final UserService userService;
    private final UserRepo userRepo;
    private final YearRepo yearRepo;
    private final OrderRepo orderRepo;
    private final AuditUserBalanceRepo auditUserBalanceRepo;

    @Override
    protected CrudRepository<AccountingEntry, String> getCrudRepository() {
        return accountingRepo;
    }

    @Override
    protected String getType() {
        return "accounting entry";
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
        log.info("Accounting entry updated for user {} (id: {})", updatedEntry.getUser().getId(), updatedEntry.getId());

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

    private BigDecimal computeTotalAmount(BigDecimal orderAmount, BigDecimal shippingCost) {
        if (shippingCost == null) {
            return orderAmount;
        }

        return orderAmount.add(shippingCost);
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

        Specification<AccountingEntry> filter = new SpecificationBuilder<AccountingEntry>()
                .withBaseFilter(AccountingSpecs.user(userId, dateAscending))
                .and(AccountingSpecs::fromDate, dateFrom)
                .and(AccountingSpecs::toDate, dateTo)
                .build();

        List<AccountingEntry> entries;
        if (skipItems != null) {
            entries = accountingRepo.findAll(filter, PageRequest.of(skipItems / maxItems, maxItems)).getContent();
        } else {
            entries = accountingRepo.findAll(filter);
        }

        Set<String> orderIds = entries.stream()
                .map(AccountingEntry::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Order> relatedOrders = getRelatedOrders(orderIds);

        List<UserBalanceEntryDTO> dtoEntries = entries.stream()
                .filter(entry -> isVisibleUserEntry(entry, relatedOrders, userId))
                .map(entry -> new UserBalanceEntryDTO().fromModel(entry, relatedOrders, userId))
                .collect(Collectors.toList());

        BigDecimal balance = userRepo.getBalance(userId);

        return new UserBalanceSummaryDTO(balance, dtoEntries);
    }

    // Evaluates if an accounting entry is visible, meaning that is actually charged to the user
    // The entry is not visible if it related to an order of a friend when summary (aggregation) is applied
    private boolean isVisibleUserEntry(AccountingEntry entry, Map<String, Order> orders, String userId) {
        if (entry.getOrderId() == null) {
            return true;
        }

        if (userId.equals(entry.getUser().getId())) {
            return true;
        }

        return !orders.get(entry.getOrderId()).getOrderType().isSummaryRequired();
    }

    private Map<String, Order> getRelatedOrders(Set<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(Iterables.partition(orderIds, 2000).spliterator(), false)
                .flatMap(orderIdsPartition -> orderRepo.findByIdIn(orderIdsPartition).stream())
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
    public void accountOrder(Order order, List<UserOrderSummary> userOrderSummaries) {
        Map<String, BigDecimal> shippingCostsByUserId = shippingCostRepo.findByOrderId(order.getId()).stream()
                .collect(Collectors.toMap(ShippingCost::getUserId, ShippingCost::getAmount));

        boolean accountingEntryConfirmed = !order.getOrderType().isComputedAmount(); //for backward compatibility and verification

        List<AccountingEntry> orderAccountingEntries = userOrderSummaries.stream()
                .map(s -> buildAccountingEntryForOrder(order, s.getUserId(), s.getFriendReferralId(), s.getTotalAmount(), shippingCostsByUserId.get(s.getUserId()), accountingEntryConfirmed))
                .collect(Collectors.toList());

        accountingRepo.saveAll(orderAccountingEntries);

        updateUserBalances(order.getId(), orderAccountingEntries, true);
    }

    @Transactional
    public void undoAccountOrder(Order order) {
        List<AccountingEntry> orderAccountingEntries = accountingRepo.findByOrderId(order.getId());
        accountingRepo.deleteAll(orderAccountingEntries);
        updateUserBalances(order.getId(), orderAccountingEntries, false);
    }

    @Transactional
    public void updateFriendBalancesFromOrderItems(String referralId, Order order) {
        List<AccountingEntry> friendOrderAccountingEntries = accountingRepo.findByOrderIdAndFriendReferralId(order.getId(), referralId);

        updateUserBalances(order.getId(), friendOrderAccountingEntries, false);
        accountingRepo.deleteAll(friendOrderAccountingEntries);

        List<OrderUserTotal> orderUserTotals = orderRepo.getComputedOrderTotalsForFriendAccounting(referralId, order.getId());
        if (orderUserTotals.isEmpty()) {
            return;
        }

        List<AccountingEntry> orderAccountingEntries = createAccountingEntriesForComputedOrder(order, orderUserTotals);
        updateUserBalances(order.getId(), orderAccountingEntries, true);
    }

    private List<AccountingEntry> createAccountingEntriesForComputedOrder(Order order, List<OrderUserTotal> orderUserTotals) {
        List<AccountingEntry> orderAccountingEntries = orderUserTotals.stream()
                .map(t -> buildAccountingEntryForOrder(order, t.getUserId(), t.getFriendReferralId(), t.getAmount(), t.getShippingCost(), false))
                .collect(Collectors.toList());

        accountingRepo.saveAll(orderAccountingEntries);

        return orderAccountingEntries;
    }

    private AccountingEntry buildAccountingEntryForOrder(Order order, String userId, String friendReferralId,
                                                         BigDecimal amount, BigDecimal shippingCost, boolean confirmed) {

        User user = User.fromId(userId, friendReferralId);

        AccountingEntry entry = new AccountingEntry();
        entry.setUser(user);
        entry.setOrderId(order.getId());
        entry.setDate(order.getDeliveryDate());
        entry.setDescription("Totale ordine " + order.getOrderType().getDescription() + " in consegna " + ConfigurationService.formatDate(order.getDeliveryDate()));

        String accountingReasonCode = confirmed ? "ORDINE" : "ORDINE_CAL";
        entry.setReason(new AccountingEntryReason().withReasonCode(accountingReasonCode));

        //used to compare previous way to aggregate balance "schedacontabile"
        entry.setConfirmed(confirmed);

        if (user.getRoleEnum().isFriend() && user.getFriendReferral() != null) {
            entry.setFriendReferralId(user.getFriendReferral().getId());
        }

        BigDecimal totalAmount = computeTotalAmount(amount, shippingCost);
        entry.setAmount(totalAmount);

        return entry;
    }

    private void updateUserBalances(String orderId, List<AccountingEntry> orderUserTotals, boolean charge) {
        BinaryOperator<BigDecimal> reduceOperator = charge ? BigDecimal::subtract : BigDecimal::add;
        OperationType operationType = charge ? OperationType.ADD : OperationType.REMOVE;

        for (AccountingEntry orderUserTotal : orderUserTotals) {
            BigDecimal cumulativeAmount = orderUserTotals.stream()
                    .filter(nestedOrderUserTotal -> isUserOrFriend(nestedOrderUserTotal, orderUserTotal.getUser().getId()))
                    .map(AccountingEntry::getAmount)
                    .reduce(BigDecimal.ZERO, reduceOperator);

            updateBalance(orderUserTotal.getUser().getId(), cumulativeAmount, orderId, EntryType.ORDER, operationType);
        }
    }

    private boolean isUserOrFriend(AccountingEntry accountingEntry, String userId) {
        return userId.equals(accountingEntry.getUser().getId()) || userId.equals(accountingEntry.getFriendReferralId());
    }

    private synchronized void updateBalance(String userId, BigDecimal amount, String entryId, EntryType entryType,
                                            OperationType operationType) {

        BigDecimal currentBalance = userRepo.getBalance(userId);

        userRepo.updateBalance(userId, amount);

        AuditUserBalance auditUserBalance = new AuditUserBalance();
        auditUserBalance.setId(UUID.randomUUID().toString());
        auditUserBalance.setUserId(userId);
        auditUserBalance.setTs(LocalDateTime.now());
        auditUserBalance.setEntryType(entryType);
        auditUserBalance.setOperationType(operationType);
        auditUserBalance.setReferenceId(entryId);
        auditUserBalance.setAmount(amount);
        auditUserBalance.setCurrentBalance(currentBalance);

        auditUserBalanceRepo.save(auditUserBalance);
    }

    public List<UserTransactionFull> getAllEntriesByUser(String userId) {
        Map<String, AccountingEntryReason> reasonsByCode = accountingReasonRepo.findAllByOrderByDescription().stream()
                .collect(toMap(AccountingEntryReason::getReasonCode));

        return accountingRepo.findByUserId(userId).stream()
                .map(entry -> new UserTransactionFull(entry, reasonsByCode.get(entry.getReason().getReasonCode())))
                .collect(Collectors.toList());
    }
}
