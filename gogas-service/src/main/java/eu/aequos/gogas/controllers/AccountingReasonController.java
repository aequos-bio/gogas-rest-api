package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.AccountingReasonService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/accounting/reason")
public class AccountingReasonController {

    private AccountingReasonService accountingReasonService;

    public AccountingReasonController(AccountingReasonService accountingReasonService) {
        this.accountingReasonService = accountingReasonService;
    }

    @GetMapping(value = "select", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<SelectItemDTO> listReasonsForSelect() {
        return accountingReasonService.getAccountingReasonsForSelect();
    }

    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryReason> getAccountingReasons() {
        return accountingReasonService.getAccountingReasons();
    }

    @IsAdmin
    @PostMapping
    public BasicResponseDTO createReason(@RequestBody AccountingEntryReason accountingEntryReason) {
        String entryId =  accountingReasonService.createOrUpdate(accountingEntryReason);
        return new BasicResponseDTO(entryId);
    }

    @IsAdmin
    @PutMapping
    public BasicResponseDTO updateReason(@RequestBody AccountingEntryReason accountingEntryReason) {
        String entryId =  accountingReasonService.createOrUpdate(accountingEntryReason);
        return new BasicResponseDTO(entryId);
    }

    @IsAdmin
    @DeleteMapping(value = "{reasonCode}")
    public BasicResponseDTO deleteReason(@PathVariable String reasonCode) {
        accountingReasonService.delete(reasonCode);
        return new BasicResponseDTO("OK");
    }
}
