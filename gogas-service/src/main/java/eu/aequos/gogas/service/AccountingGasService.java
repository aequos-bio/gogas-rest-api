package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import eu.aequos.gogas.persistence.repository.AccountingGasRepo;
import eu.aequos.gogas.persistence.specification.AccountingGasSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountingGasService extends CrudService<AccountingGasEntry, String> {

    private AccountingGasRepo accountingGasRepo;

    public AccountingGasService(AccountingGasRepo accountingRepo) {
        super(accountingRepo, "accounting gas entry");
        this.accountingGasRepo = accountingRepo;
    }

    public List<AccountingGasEntryDTO> getAccountingEntries(String reasonCode,
                                                         String description, Date dateFrom, Date dateTo) {

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
