package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsAdminOrCurrentUser;
import eu.aequos.gogas.service.AccountingGasService;
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
@IsAdmin
public class AccountingController {

    private AccountingService accountingService;
    private AccountingGasService accountingGasService;
    private ConfigurationService configurationService;
    private AccountingReasonService accountingReasonService;

    public AccountingController(AccountingService accountingService, AccountingGasService accountingGasService,
                                ConfigurationService configurationService, AccountingReasonService accountingReasonService) {

        this.accountingService = accountingService;
        this.accountingGasService = accountingGasService;
        this.configurationService = configurationService;
        this.accountingReasonService = accountingReasonService;
    }

    @GetMapping(value = "{userId}/balance", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BigDecimal getBalance(@PathVariable String userId) {
        return accountingService.getBalance(userId);
    }



    @GetMapping(value = "entry/user/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryDTO> getAccountingEntries(@RequestParam(required = false) String userId,
                                                         @RequestParam(required = false) String reasonCode,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String dateFromParam,
                                                         @RequestParam(required = false) String dateToParam) {

        Date dateFrom = configurationService.parseDate(dateFromParam);
        Date dateTo = configurationService.parseDate(dateToParam);

        return accountingService.getAccountingEntries(userId, reasonCode, description, dateFrom, dateTo);
    }

    @PostMapping(value = "entry/user")
    public BasicResponseDTO create(@RequestBody AccountingEntryDTO accountingEntryDTO) {
        String entryId =  accountingService.create(accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/user/{accountingEntryId}")
    public BasicResponseDTO update(@PathVariable String accountingEntryId, @RequestBody AccountingEntryDTO accountingEntryDTO) throws ItemNotFoundException {
        String entryId =  accountingService.update(accountingEntryId, accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "entry/user/{accountingEntryId}")
    public BasicResponseDTO delete(@PathVariable String accountingEntryId) {
        accountingService.delete(accountingEntryId);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "entry/gas/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingGasEntryDTO> getGasEntries(@RequestParam(required = false) String reasonCode,
                                                               @RequestParam(required = false) String description,
                                                               @RequestParam(required = false) String dateFromParam,
                                                               @RequestParam(required = false) String dateToParam) {

        Date dateFrom = configurationService.parseDate(dateFromParam);
        Date dateTo = configurationService.parseDate(dateToParam);

        return accountingGasService.getAccountingEntries(reasonCode, description, dateFrom, dateTo);
    }

    @PostMapping(value = "entry/gas")
    public BasicResponseDTO createGasEntry(@RequestBody AccountingGasEntryDTO accountingGasEntryDTO) {
        String entryId = accountingGasService.create(accountingGasEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/gas/{accountingGasEntryId}")
    public BasicResponseDTO updateGasEntry(@PathVariable String accountingGasEntryId, @RequestBody AccountingGasEntryDTO accountingGasEntryDTO) throws ItemNotFoundException {
        String entryId = accountingGasService.update(accountingGasEntryId, accountingGasEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "entry/gas/{accountingGasEntryId}")
    public BasicResponseDTO deleteGasAccountingEntry(@PathVariable String accountingGasEntryId) {
        accountingGasService.delete(accountingGasEntryId);
        return new BasicResponseDTO("OK");
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
    public BasicResponseDTO createReason(@RequestBody AccountingEntryReason accountingEntryReason) {
        String entryId =  accountingReasonService.createOrUpdate(accountingEntryReason);
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "reason")
    public BasicResponseDTO updateReason(@RequestBody AccountingEntryReason accountingEntryReason) {
        String entryId =  accountingReasonService.createOrUpdate(accountingEntryReason);
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "reason/{reasonCode}")
    public BasicResponseDTO deleteReason(@PathVariable String reasonCode) {
        accountingReasonService.delete(reasonCode);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "balance/users")
    public List<UserBalanceDTO> getUserBalanceList() {
        return accountingService.getUserBalanceList();
    }

    @IsAdminOrCurrentUser
    @GetMapping(value = "balance/users/{userId}")
    public UserBalanceSummaryDTO getUserBalance(@PathVariable String userId,
                                                @RequestParam(required = false) String dateFromParam,
                                                @RequestParam(required = false) String dateToParam) {

        Date dateFrom = configurationService.parseDate(dateFromParam);
        Date dateTo = configurationService.parseDate(dateToParam);

        return accountingService.getUserBalance(userId, dateFrom, dateTo);
    }
}
