package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.specification.AccountingSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountingService extends CrudService<AccountingEntry, String> {

    private AccountingRepo accountingRepo;

    public AccountingService(AccountingRepo accountingRepo) {
        super(accountingRepo, "accounting entry");
        this.accountingRepo = accountingRepo;
    }

    public BigDecimal getBalance(String userId) {
        return accountingRepo.getBalance(userId);
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
}
