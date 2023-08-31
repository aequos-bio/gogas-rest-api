package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.dto.OrderAccountingInfoDTO;
import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import eu.aequos.gogas.persistence.entity.derived.OrderTotal;
import eu.aequos.gogas.persistence.repository.AccountingGasRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import eu.aequos.gogas.persistence.specification.AccountingGasSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class AccountingGasService extends CrudService<AccountingGasEntry, String> {

    private final AccountingGasRepo accountingGasRepo;
    private final AccountingService accountingService;
    private final OrderRepo orderRepo;
    private final UserOrderSummaryRepo userOrderSummaryRepo;

    @Override
    protected CrudRepository<AccountingGasEntry, String> getCrudRepository() {
        return accountingGasRepo;
    }

    @Override
    protected String getType() {
        return "accounting gas entry";
    }

    public AccountingGasEntry create(AccountingGasEntryDTO dto) throws GoGasException {
        if (accountingService.isYearClosed(dto.getDate()))
            throw new GoGasException("Il movimento non può essere creato, l'anno contabile è chiuso");

        return super.create(dto);
    }

    public AccountingGasEntry update(String entryId, AccountingGasEntryDTO dto) throws ItemNotFoundException, GoGasException {
        AccountingGasEntry existingEntry = getRequired(entryId);
        if (accountingService.isYearClosed(existingEntry.getDate()) || accountingService.isYearClosed(dto.getDate()))
            throw new GoGasException("Il movimento non può essere modificato, l'anno contabile è chiuso");

        return super.createOrUpdate(existingEntry, dto);
    }

    public List<AccountingGasEntryDTO> getManualAccountingEntries(String reasonCode, String description,
                                                                  LocalDate dateFrom, LocalDate dateTo) {

        Specification<AccountingGasEntry> filter = new SpecificationBuilder<AccountingGasEntry>()
                .withBaseFilter(AccountingGasSpecs.select())
                .and(AccountingGasSpecs::reason, reasonCode)
                .and(AccountingGasSpecs::descriptionLike, description)
                .and(AccountingGasSpecs::fromDate, dateFrom)
                .and(AccountingGasSpecs::toDate, dateTo)
                .build();

        return accountingGasRepo.findAll(filter).stream()
                .map(entry -> new AccountingGasEntryDTO().fromModel(entry))
                .collect(Collectors.toList());
    }

    public List<AccountingGasEntryDTO> getAccountingEntriesInYear(int year) {
        LocalDate dateFrom = LocalDate.of(year, 1, 1);
        LocalDate dateTo = LocalDate.of(year, 12, 31);

        Stream<AccountingGasEntryDTO> manualEntries = accountingGasRepo.findByDateBetween(dateFrom, dateTo).stream()
                .map(entry -> new AccountingGasEntryDTO().fromModel(entry));

        Stream<AccountingGasEntryDTO> orderEntries = getOrderAccontingEntries(dateFrom, dateTo).stream();

        List<OrderTotal> totals = userOrderSummaryRepo.getOrderTotals(dateFrom, dateTo);
        Stream<AccountingGasEntryDTO> orderTotals = totals.stream()
            .map(total -> new AccountingGasEntryDTO().fromOrderTotal(total));

        return Stream.concat(Stream.concat(manualEntries, orderEntries), orderTotals)
                .sorted(Comparator.comparing(AccountingGasEntryDTO::getDate))
                .collect(Collectors.toList());
    }

    private List<AccountingGasEntryDTO> getOrderAccontingEntries(LocalDate dateFrom, LocalDate dateTo) {
        //getting unique by invoice key (accountingCode, invoiceNumber, invoiceDate) to avoid duplicating aequos invoices for multiple orders
        Collection<OrderAccountingInfoDTO> orderList = orderRepo.findAccountedByInvoiceDateBetween(dateFrom, dateTo).stream()
                .map(o -> new OrderAccountingInfoDTO().fromOrder(o))
                .collect(Collectors.toMap(OrderAccountingInfoDTO::getInvoiceKey, Function.identity(), OrderAccountingInfoDTO::mergeByInvoiceKey))
                .values();

        List<AccountingGasEntryDTO> accountingGasEntryDTOs = new ArrayList<>();
        for (OrderAccountingInfoDTO order : orderList) {
            accountingGasEntryDTOs.add(new AccountingGasEntryDTO().fromOrderInvoice(order));

            if (order.isPaid() && order.getPaymentDate() != null)
                accountingGasEntryDTOs.add(new AccountingGasEntryDTO().fromOrderPayment(order));
        }

        return accountingGasEntryDTOs;
    }

    public List<OrderAccountingInfoDTO> getOrderAccontingInfos(LocalDate dateFrom, LocalDate dateTo) {
        //getting unique by invoice key (accountingCode, invoiceNumber, invoiceDate) to avoid duplicating aequos invoices for multiple orders
        Collection<OrderAccountingInfoDTO> infoList = orderRepo.findAccountedByInvoiceDateBetween(dateFrom, dateTo)
                .stream()
                .map(o -> new OrderAccountingInfoDTO().fromOrder(o))
                .collect(Collectors.toMap(OrderAccountingInfoDTO::getInvoiceKey, Function.identity(), OrderAccountingInfoDTO::mergeByInvoiceKey))
                .values();

        return new ArrayList<>(infoList);
    }

    public List<OrderDTO> getOrdersWithoutInvoice(LocalDate dateFrom, LocalDate dateTo, boolean aequosOrders) {
        return orderRepo.findAccountedOrdersWithoutInvoice(dateFrom, dateTo, aequosOrders)
                .stream()
                .map(o -> new OrderDTO().fromModel(o))
                .collect(Collectors.toList());
    }
}
