package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AccountingEntryDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.IsCurrentUserFriend;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/accounting/friend")
public class AccountingFriendController {

    private AccountingService accountingService;
    private ConfigurationService configurationService;
    private AuthorizationService authorizationService;

    public AccountingFriendController(AccountingService accountingService, ConfigurationService configurationService,
                                      AuthorizationService authorizationService) {

        this.accountingService = accountingService;
        this.configurationService = configurationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "entry/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingEntryDTO> getAccountingEntries(@RequestParam(required = false) String userId,
                                                         @RequestParam(required = false) String reasonCode,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String dateFrom,
                                                         @RequestParam(required = false) String dateTo) {

        String currentUserId = authorizationService.getCurrentUser().getId();
        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingService.getAccountingEntries(userId, reasonCode, description, parsedDateFrom, parsedDateTo, currentUserId);
    }

    @PostMapping(value = "entry")
    public BasicResponseDTO createAccountingEntry(@RequestBody AccountingEntryDTO accountingEntryDTO) throws UserNotAuthorizedException {
        if (!authorizationService.isFriend(accountingEntryDTO.getUserId()))
            throw new UserNotAuthorizedException();

        //forcing current logged user as friend referral
        accountingEntryDTO.setFriendReferralId(authorizationService.getCurrentUser().getId());

        String entryId =  accountingService.create(accountingEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/{accountingEntryId}")
    public BasicResponseDTO updateAccountingEntry(@PathVariable String accountingEntryId, @RequestBody AccountingEntryDTO accountingEntryDTO) throws ItemNotFoundException, UserNotAuthorizedException {
        if (!isFriendAccountingEntry(accountingEntryId))
            throw new UserNotAuthorizedException();

        //forcing current logged user as friend referral
        accountingEntryDTO.setFriendReferralId(authorizationService.getCurrentUser().getId());

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
                                                @RequestParam(required = false) String dateTo) {

        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingService.getUserBalance(userId, parsedDateFrom, parsedDateTo);
    }

    private boolean isFriendAccountingEntry(@PathVariable String accountingEntryId) {
        String accountingEntryUserId = accountingService.getRequired(accountingEntryId).getUser().getId();
        return authorizationService.isFriend(accountingEntryUserId);
    }
}
