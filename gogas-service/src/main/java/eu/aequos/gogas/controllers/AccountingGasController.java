package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.AccountingGasService;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/accounting/gas")
@IsAdmin
public class AccountingGasController {

    private AccountingGasService accountingGasService;
    private ConfigurationService configurationService;

    public AccountingGasController(AccountingGasService accountingGasService,
                                   ConfigurationService configurationService) {

        this.accountingGasService = accountingGasService;
        this.configurationService = configurationService;
    }

    @GetMapping(value = "entry/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingGasEntryDTO> getGasEntries(@RequestParam(required = false) String reasonCode,
                                                               @RequestParam(required = false) String description,
                                                               @RequestParam(required = false) String dateFrom,
                                                               @RequestParam(required = false) String dateTo) {

        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingGasService.getAccountingEntries(reasonCode, description, parsedDateFrom, parsedDateTo);
    }

    @PostMapping(value = "entry")
    public BasicResponseDTO createGasEntry(@RequestBody AccountingGasEntryDTO accountingGasEntryDTO) throws GoGasException {
        String entryId = accountingGasService.create(accountingGasEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/{accountingGasEntryId}")
    public BasicResponseDTO updateGasEntry(@PathVariable String accountingGasEntryId, @RequestBody AccountingGasEntryDTO accountingGasEntryDTO) throws ItemNotFoundException, GoGasException {
        String entryId = accountingGasService.update(accountingGasEntryId, accountingGasEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "entry/{accountingGasEntryId}")
    public BasicResponseDTO deleteGasAccountingEntry(@PathVariable String accountingGasEntryId) {
        accountingGasService.delete(accountingGasEntryId);
        return new BasicResponseDTO("OK");
    }
}
