package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.CanViewBalance;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/accounting/user")
public class AccountingUserController {

    private AccountingService accountingService;
    private ConfigurationService configurationService;
    private AuthorizationService authorizationService;

    public AccountingUserController(AccountingService accountingService, ConfigurationService configurationService,
                                    AuthorizationService authorizationService) {
        this.accountingService = accountingService;
        this.configurationService = configurationService;
        this.authorizationService = authorizationService;
    }

    @IsAdmin
    @GetMapping(value = "entry/{accountingEntryId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public AccountingEntryDTO getAccountingEntry(@PathVariable String accountingEntryId) {
        return accountingService.get(accountingEntryId);
    }

    @IsAdmin
    @GetMapping(value = "entry/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryDTO> getAccountingEntries(@RequestParam(required = false) String userId,
                                                         @RequestParam(required = false) String reasonCode,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String dateFrom,
                                                         @RequestParam(required = false) String dateTo) {

        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingService.getAccountingEntries(userId, reasonCode, description, parsedDateFrom, parsedDateTo, null);
    }

    @IsAdmin
    @PostMapping(value = "entry")
    public BasicResponseDTO create(@RequestBody AccountingEntryDTO accountingEntryDTO) throws GoGasException {
        String entryId =  accountingService.create(accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @IsAdmin
    @PutMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO update(@PathVariable String accountingEntryId, @RequestBody AccountingEntryDTO accountingEntryDTO) throws ItemNotFoundException, GoGasException {
        String entryId =  accountingService.update(accountingEntryId, accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @IsAdmin
    @DeleteMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO delete(@PathVariable String accountingEntryId) {
        accountingService.delete(accountingEntryId);
        return new BasicResponseDTO("OK");
    }

    @IsAdmin
    @GetMapping(value = "balance/list")
    public List<UserBalanceDTO> getUserBalanceList() {
        return accountingService.getUserBalanceList();
    }

    @CanViewBalance
    @GetMapping(value = "balance/{userId}/entries")
    public UserBalanceSummaryDTO getUserBalance(@PathVariable String userId,
                                                @RequestParam(required = false) String dateFrom,
                                                @RequestParam(required = false) String dateTo) {

        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingService.getUserBalance(userId, parsedDateFrom, parsedDateTo, false);
    }

    @CanViewBalance
    @GetMapping(value = "balance/{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BigDecimal getBalance(@PathVariable String userId) {
        return accountingService.getBalance(userId);
    }


    @GetMapping(value = "balance", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BigDecimal getBalance() {
        String userId = authorizationService.getCurrentUser().getId();
        return accountingService.getBalance(userId);
    }

    @GetMapping(value = "balance/entries/{pageNumber}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public PageResultDTO<UserBalanceEntryDTO> getPaginatedBalanceEntries(@PathVariable int pageNumber, @RequestParam int pageSize) {
        String userId = authorizationService.getCurrentUser().getId();
        return accountingService.getPaginatedBalanceEntries(userId, pageNumber, pageSize);
    }
}
