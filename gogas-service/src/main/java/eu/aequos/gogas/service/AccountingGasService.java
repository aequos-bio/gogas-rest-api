package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import eu.aequos.gogas.persistence.repository.AccountingGasRepo;
import eu.aequos.gogas.persistence.specification.AccountingGasSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountingGasService extends CrudService<AccountingGasEntry, String> {

    private AccountingGasRepo accountingGasRepo;
    private AccountingService accountingService;

    public AccountingGasService(AccountingGasRepo accountingRepo, AccountingService accountingService) {
        super(accountingRepo, "accounting gas entry");
        this.accountingGasRepo = accountingRepo;
        this.accountingService = accountingService;
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

    public List<AccountingGasEntryDTO> getAccountingEntries(String reasonCode,
                                                            String description, LocalDate dateFrom, LocalDate dateTo) {

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
}
