package eu.aequos.gogas.controllers;

import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.service.AccountingReasonService;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("accounting")
public class AccountingController {

    private AccountingService accountingService;
    private ConfigurationService configurationService;
    private AccountingReasonService accountingReasonService;

    public AccountingController(AccountingService accountingService, ConfigurationService configurationService,
                                AccountingReasonService accountingReasonService) {

        this.accountingService = accountingService;
        this.configurationService = configurationService;
        this.accountingReasonService = accountingReasonService;
    }

    @GetMapping(value = "{userId}/balance", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BigDecimal getBalance(@PathVariable String userId) {
        return accountingService.getBalance(userId);
    }


    @GetMapping(value = "entry/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryDTO> getAccountingEntries(@RequestParam(required = false) String userId,
                                                         @RequestParam(required = false) String reasonCode,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String dateFromParam,
                                                         @RequestParam(required = false) String dateToParam) {

        Date dateFrom = configurationService.parseDate(dateFromParam);
        Date dateTo = configurationService.parseDate(dateToParam);

        return accountingService.getAccountingEntries(userId, reasonCode, description, dateFrom, dateTo);
    }

    @PostMapping(value = "entry")
    public String create(@RequestBody AccountingEntryDTO accountingEntryDTO) {
        return accountingService.create(accountingEntryDTO).getId();
    }

    @PutMapping(value = "entry/{accountingEntryId}")
    public String update(@PathVariable String accountingEntryId, @RequestBody AccountingEntryDTO accountingEntryDTO) throws ItemNotFoundException {
        return accountingService.update(accountingEntryId, accountingEntryDTO).getId();
    }

    @DeleteMapping(value = "entry/{accountingEntryId}")
    public void delete(@PathVariable String accountingEntryId) {
        accountingService.delete(accountingEntryId);
    }

    @GetMapping(value = "reason/select", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<SelectItemDTO> listReasonsForSelect() {
        return accountingReasonService.getAccountingReasonsForSelect();
    }

    @GetMapping(value = "reason/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryReason> getAccountingReasons() {
        return accountingReasonService.getAccountingReasons();
    }

    @PostMapping(value = "reason")
    public String createReason(@RequestBody AccountingEntryReason accountingEntryReason) {
        return accountingReasonService.createOrUpdate(accountingEntryReason);
    }

    @PutMapping(value = "reason")
    public String updateReason(@RequestBody AccountingEntryReason accountingEntryReason) {
        return accountingReasonService.createOrUpdate(accountingEntryReason);
    }

    @DeleteMapping(value = "reason/{reasonCode}")
    public void deleteReason(@PathVariable String reasonCode) {
        accountingReasonService.delete(reasonCode);
    }
}
