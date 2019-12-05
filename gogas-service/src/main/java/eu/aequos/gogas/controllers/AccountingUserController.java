package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.CanViewBalance;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("accounting/user")
@IsAdmin
public class AccountingUserController {

    private AccountingService accountingService;
    private ConfigurationService configurationService;

    public AccountingUserController(AccountingService accountingService, ConfigurationService configurationService) {
        this.accountingService = accountingService;
        this.configurationService = configurationService;
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

        return accountingService.getAccountingEntries(userId, reasonCode, description, dateFrom, dateTo, null);
    }

    @PostMapping(value = "entry")
    public BasicResponseDTO create(@RequestBody AccountingEntryDTO accountingEntryDTO) {
        String entryId =  accountingService.create(accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO update(@PathVariable String accountingEntryId, @RequestBody AccountingEntryDTO accountingEntryDTO) throws ItemNotFoundException {
        String entryId =  accountingService.update(accountingEntryId, accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO delete(@PathVariable String accountingEntryId) {
        accountingService.delete(accountingEntryId);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "balance")
    public List<UserBalanceDTO> getUserBalanceList() {
        return accountingService.getUserBalanceList();
    }

    @CanViewBalance
    @GetMapping(value = "balance/{userId}")
    public UserBalanceSummaryDTO getUserBalance(@PathVariable String userId,
                                                @RequestParam(required = false) String dateFromParam,
                                                @RequestParam(required = false) String dateToParam) {

        Date dateFrom = configurationService.parseDate(dateFromParam);
        Date dateTo = configurationService.parseDate(dateToParam);

        return accountingService.getUserBalance(userId, dateFrom, dateTo);
    }
}
