package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.IsCurrentUserFriend;
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
import java.time.LocalDate;
import java.util.List;

@Api("Friends accounting")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("api/accounting/friend")
public class AccountingFriendController {

    private final AccountingService accountingService;
    private final ConfigurationService configurationService;
    private final AuthorizationService authorizationService;
    private final ExcelGenerationService excelGenerationService;

    @GetMapping(value = "entry/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryDTO> getAccountingEntries(@RequestParam(required = false) String userId,
                                                         @RequestParam(required = false) String reasonCode,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String dateFrom,
                                                         @RequestParam(required = false) String dateTo) {

        String currentUserId = authorizationService.getCurrentUser().getId();
        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingService.getFriendsAccountingEntries(userId, reasonCode, description, parsedDateFrom, parsedDateTo, currentUserId);
    }

    @GetMapping(value = "entry/{accountingEntryId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public AccountingEntryDTO getAccountingEntry(@PathVariable String accountingEntryId) {
        AccountingEntryDTO accountingEntryDTO = accountingService.get(accountingEntryId);

        if (!authorizationService.isFriend(accountingEntryDTO.getUserId()))
            throw new UserNotAuthorizedException();

        return accountingEntryDTO;
    }

    @PostMapping(value = "entry")
    public BasicResponseDTO createAccountingEntry(@RequestBody @Valid AccountingEntryDTO accountingEntryDTO) throws GoGasException {
        if (!authorizationService.isFriend(accountingEntryDTO.getUserId()))
            throw new UserNotAuthorizedException();

        String entryId =  accountingService.create(accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO updateAccountingEntry(@PathVariable String accountingEntryId, @RequestBody @Valid AccountingEntryDTO accountingEntryDTO) throws ItemNotFoundException, GoGasException {
        if (!isFriendAccountingEntry(accountingEntryId))
            throw new UserNotAuthorizedException();

        String entryId =  accountingService.update(accountingEntryId, accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO deleteAccountingEntry(@PathVariable String accountingEntryId) throws UserNotAuthorizedException {
        if (!isFriendAccountingEntry(accountingEntryId))
            throw new UserNotAuthorizedException();

        accountingService.delete(accountingEntryId);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "balance")
    public List<UserBalanceDTO> getUserBalanceList() {
        return accountingService.getFriendBalanceList(authorizationService.getCurrentUser().getId());
    }

    @IsCurrentUserFriend
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

    private boolean isFriendAccountingEntry(@PathVariable String accountingEntryId) {
        String accountingEntryUserId = accountingService.getRequired(accountingEntryId).getUser().getId();
        return authorizationService.isFriend(accountingEntryUserId);
    }

    @GetMapping(value = "/exportTotals", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserTotals(HttpServletResponse response,
                                                 @RequestParam(name = "includeUsers", required = false) boolean includeUsers) throws Exception {

        String currentUserId = authorizationService.getCurrentUser().getId();
        return excelGenerationService.exportFriendTotals(currentUserId, includeUsers);
    }

    @IsCurrentUserFriend
    @GetMapping(value = "/exportDetails", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserDetails(HttpServletResponse response, @RequestParam(name = "userId") String userId) throws Exception {
        byte[] bytes = excelGenerationService.exportUserEntries(userId);
        if (bytes == null)
            response.sendError(406);
        return bytes;
    }
}
