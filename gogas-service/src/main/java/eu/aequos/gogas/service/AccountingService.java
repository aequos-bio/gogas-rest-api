package eu.aequos.gogas.service;

import com.google.common.collect.Iterables;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static eu.aequos.gogas.converter.ListConverter.toMap;

@Slf4j
@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private AccountingRepo accountingRepo;
    private UserBalanceRepo userBalanceRepo;
    private UserBalanceEntryRepo userBalanceEntryRepo;
    private UserService userService;
    private YearRepo yearRepo;
    private OrderRepo orderRepo;

    public AccountingService(AccountingRepo accountingRepo, UserBalanceRepo userBalanceRepo,
                             UserBalanceEntryRepo userBalanceEntryRepo, UserService userService,
                             YearRepo yearRepo, OrderRepo orderRepo) {

        super(accountingRepo, "accounting entry");
        this.accountingRepo = accountingRepo;
        this.userBalanceRepo = userBalanceRepo;
        this.userBalanceEntryRepo = userBalanceEntryRepo;
        this.userService = userService;
        this.yearRepo = yearRepo;
        this.orderRepo = orderRepo;
    }

    public BigDecimal getBalance(String userId) {
        return userBalanceRepo.getBalance(userId)
                .orElse(BigDecimal.ZERO);
    }

    public AccountingEntry create(AccountingEntryDTO dto) throws GoGasException {
        if (isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere creato, l'anno contabile è chiuso");

        AccountingEntry accountingEntry = super.create(dto);
        log.info("New accounting entry created for user {} (id: {})", accountingEntry.getUser().getId(), accountingEntry.getId());

        return accountingEntry;
    }

    public AccountingEntryDTO get(String entryId) {
        AccountingEntry existingEntry = getRequired(entryId);
        return new AccountingEntryDTO().fromModel(existingEntry);
    }

    public AccountingEntry update(String entryId, AccountingEntryDTO dto) throws ItemNotFoundException, GoGasException {
        AccountingEntry existingEntry = getRequired(entryId);

        if (existingEntry.getOrderId() != null)
            throw new GoGasException("Movimento relativo ad un ordine, non può essere modificato");

        if (isYearClosed(existingEntry) || isYearClosed(dto))
            throw new GoGasException("Il movimento non può essere modificato, l'anno contabile è chiuso");

        AccountingEntry updateEntry = super.createOrUpdate(existingEntry, dto);
        log.info("Accounting entry updated for user {} (id: {})", updateEntry.getUser().getId(), updateEntry.getId());

        return updateEntry;
    }

    @Override
    public void delete(String entryId) {
        AccountingEntry existingEntry = getRequired(entryId);

        if (existingEntry.getOrderId() != null)
            throw new GoGasException("Movimento relativo ad un ordine, non può essere eliminato");

        if (isYearClosed(existingEntry) || isYearClosed(existingEntry))
            throw new GoGasException("Il movimento non può essere eliminato, l'anno contabile è chiuso");

        super.delete(entryId);
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
        return toUserBalanceDTO(userBalanceRepo.findAllByRole(User.Role.U.name()));
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

        BigDecimal balance = userBalanceRepo.getBalance(userId)
                .orElse(BigDecimal.ZERO);

        return new UserBalanceSummaryDTO(balance, dtoEntries);
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

    public int setEntriesConfirmedByOrderId(String orderId, boolean confirmed) {
        return accountingRepo.setConfirmedByOrderId(orderId, confirmed);
    }

    public long countEntriesByOrderId(String orderId) {
        return accountingRepo.countByOrderId(orderId);
    }
}
