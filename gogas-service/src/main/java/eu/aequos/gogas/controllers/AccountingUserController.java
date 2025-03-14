package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.CanViewBalance;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsManager;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.ExcelGenerationService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Api("Users accounting")
@RestController
@RequestMapping("api/accounting/user")
@Validated
@IsAdmin
@RequiredArgsConstructor
public class AccountingUserController {

    private final AccountingService accountingService;
    private final ConfigurationService configurationService;
    private final ExcelGenerationService excelGenerationService;

    @CanViewBalance
    @GetMapping(value = "{userId}/balance", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BigDecimal getBalance(@PathVariable String userId) {
        return accountingService.getBalance(userId);
    }

    @GetMapping(value = "entry/{accountingEntryId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public AccountingEntryDTO getAccountingEntry(@PathVariable String accountingEntryId) {
        return accountingService.get(accountingEntryId);
    }

    @GetMapping(value = "entry/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryDTO> getAccountingEntries(@RequestParam(required = false) String userId,
                                                         @RequestParam(required = false) String reasonCode,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String dateFrom,
                                                         @RequestParam(required = false) String dateTo) {

        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingService.getUserAccountingEntries(userId, reasonCode, description, parsedDateFrom, parsedDateTo);
    }

    @PostMapping(value = "entry")
    public BasicResponseDTO create(@RequestBody @Valid AccountingEntryDTO accountingEntryDTO) throws GoGasException {
        String entryId =  accountingService.create(accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO update(@PathVariable String accountingEntryId, @RequestBody @Valid AccountingEntryDTO accountingEntryDTO) throws ItemNotFoundException, GoGasException {
        String entryId =  accountingService.update(accountingEntryId, accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO delete(@PathVariable String accountingEntryId) {
        accountingService.delete(accountingEntryId);
        return new BasicResponseDTO("OK");
    }

    @IsManager
    @GetMapping(value = "balance")
    public List<UserBalanceDTO> getUserBalanceList() {
        return accountingService.getUserBalanceList();
    }

    @CanViewBalance
    @GetMapping(value = "balance/{userId}")
    public UserBalanceSummaryDTO getUserBalance(@PathVariable String userId,
                                                @RequestParam(required = false) String dateFrom,
                                                @RequestParam(required = false) String dateTo,
                                                @RequestParam(required = false) Integer skipItems,
                                                @RequestParam(required = false) Integer maxItems) {

        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingService.getPaginatedUserBalance(userId, parsedDateFrom, parsedDateTo, false, skipItems, maxItems);
    }

    @GetMapping(value = "/exportTotals", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserTotals(HttpServletResponse response,
                                                 @RequestParam(name = "includeUsers", required = false) boolean includeUsers) throws Exception {

        return excelGenerationService.exportUserTotals(includeUsers);
    }

    @CanViewBalance
    @GetMapping(value = "/exportDetails", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserDetails(HttpServletResponse response, @RequestParam(name = "userId") String userId) throws Exception {
        byte[] bytes = excelGenerationService.exportUserEntries(userId);
        if (bytes == null)
            response.sendError(406);
        return bytes;
    }
}
