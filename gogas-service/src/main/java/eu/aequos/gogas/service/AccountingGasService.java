package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.AccountingGasRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.specification.AccountingGasSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AccountingGasService extends CrudService<AccountingGasEntry, String> {

    private AccountingGasRepo accountingGasRepo;
    private AccountingService accountingService;
    private OrderRepo orderRepo;

    public AccountingGasService(AccountingGasRepo accountingRepo, AccountingService accountingService,
                                OrderRepo orderRepo) {

        super(accountingRepo, "accounting gas entry");
        this.accountingGasRepo = accountingRepo;
        this.accountingService = accountingService;
        this.orderRepo = orderRepo;
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

        return Stream.concat(manualEntries, orderEntries)
                .sorted(Comparator.comparing(AccountingGasEntryDTO::getDate))
                .collect(Collectors.toList());
    }

    private List<AccountingGasEntryDTO> getOrderAccontingEntries(LocalDate dateFrom, LocalDate dateTo) {
        List<Order> orderList = orderRepo.findAccountedByInvoiceDateBetween(dateFrom, dateTo);

        List<AccountingGasEntryDTO> accountingGasEntryDTOS = new ArrayList<>();
        for (Order order : orderList) {
            accountingGasEntryDTOS.add(new AccountingGasEntryDTO().fromOrderInvoice(order));

            if (order.isPaid() && order.getPaymentDate() != null)
                accountingGasEntryDTOS.add(new AccountingGasEntryDTO().fromOrderPayment(order));
        }

        return accountingGasEntryDTOS;
    }
}
