package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.AccountingGasService;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("accounting/gas")
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
                                                               @RequestParam(required = false) String dateFromParam,
                                                               @RequestParam(required = false) String dateToParam) {

        Date dateFrom = configurationService.parseDate(dateFromParam);
        Date dateTo = configurationService.parseDate(dateToParam);

        return accountingGasService.getAccountingEntries(reasonCode, description, dateFrom, dateTo);
    }

    @PostMapping(value = "entry")
    public BasicResponseDTO createGasEntry(@RequestBody AccountingGasEntryDTO accountingGasEntryDTO) {
        String entryId = accountingGasService.create(accountingGasEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @PutMapping(value = "entry/{accountingGasEntryId}")
    public BasicResponseDTO updateGasEntry(@PathVariable String accountingGasEntryId, @RequestBody AccountingGasEntryDTO accountingGasEntryDTO) throws ItemNotFoundException {
        String entryId = accountingGasService.update(accountingGasEntryId, accountingGasEntryDTO).getId();
        return new BasicResponseDTO(entryId);
    }

    @DeleteMapping(value = "entry/{accountingGasEntryId}")
    public BasicResponseDTO deleteGasAccountingEntry(@PathVariable String accountingGasEntryId) {
        accountingGasService.delete(accountingGasEntryId);
        return new BasicResponseDTO("OK");
    }
}
