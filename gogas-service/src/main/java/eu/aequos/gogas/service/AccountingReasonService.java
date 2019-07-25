package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.SelectItemsConverter;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.repository.AccountingReasonRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class AccountingReasonService extends CrudService<AccountingEntryReason, String> {

    private static final Function<AccountingEntryReason, SelectItemDTO> SELECT_ITEM_CONVERSION = t -> new SelectItemDTO(t.getReasonCode(), t.getDescription());

    private AccountingReasonRepo accountingReasonRepo;
    private SelectItemsConverter selectItemsConverter;

    public AccountingReasonService(AccountingReasonRepo accountingReasonRepo, SelectItemsConverter selectItemsConverter) {
        super(accountingReasonRepo);

        this.accountingReasonRepo = accountingReasonRepo;
        this.selectItemsConverter = selectItemsConverter;
    }

    public List<SelectItemDTO> getAccountingReasonsForSelect() {
        Stream<AccountingEntryReason> reasons = accountingReasonRepo.findAllByOrderByDescription().stream();
        return selectItemsConverter.toSelectItems(reasons, SELECT_ITEM_CONVERSION, false, null);
    }

    public List<AccountingEntryReason> getAccountingReasons() {
        return accountingReasonRepo.findAllByOrderByDescription();
    }

    public String createOrUpdate(AccountingEntryReason accountingEntryReason) {
        return accountingReasonRepo.save(accountingEntryReason).getReasonCode();
    }
}
