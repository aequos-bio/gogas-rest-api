package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.AccountingGasService;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.ExcelGenerationService;
import eu.aequos.gogas.service.OrderManagerService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Api("GAS accounting")
@RestController
@RequestMapping("api/accounting/gas")
@IsAdmin
public class AccountingGasController {

    private AccountingGasService accountingGasService;
    private ConfigurationService configurationService;
    private ExcelGenerationService excelGenerationService;
    private OrderManagerService orderManagerService;

    public AccountingGasController(AccountingGasService accountingGasService,
                                   ConfigurationService configurationService,
                                   ExcelGenerationService excelGenerationService,
                                   OrderManagerService orderManagerService) {

        this.accountingGasService = accountingGasService;
        this.configurationService = configurationService;
        this.excelGenerationService = excelGenerationService;
        this.orderManagerService = orderManagerService;
    }

    @GetMapping(value = "entry/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<AccountingGasEntryDTO> getGasEntries(@RequestParam(required = false) String reasonCode,
                                                               @RequestParam(required = false) String description,
                                                               @RequestParam(required = false) String dateFrom,
                                                               @RequestParam(required = false) String dateTo) {

        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);

        return accountingGasService.getManualAccountingEntries(reasonCode, description, parsedDateFrom, parsedDateTo);
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

    @GetMapping(value = "report/{year}")
    public List<AccountingGasEntryDTO> allAccountingEntriesByYear(@PathVariable int year) {
        return accountingGasService.getAccountingEntriesInYear(year);
    }


    @GetMapping(value = "report/{year}/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody
    byte[] exportGasDetails(HttpServletResponse response,
                            @PathVariable int year) throws Exception {

        List<AccountingGasEntryDTO> entriesInYear = accountingGasService.getAccountingEntriesInYear(year);
        return excelGenerationService.exportGasEntries(entriesInYear);

    }

    @GetMapping(value="invoices/{year}")
    public List<OrderAccountingInfoDTO> getAllInvoices(@PathVariable int year) {
        LocalDate dateFrom = LocalDate.of(year, 1, 1);
        LocalDate dateTo = LocalDate.of(year, 12, 31);
        return accountingGasService.getOrderAccontingInfos(dateFrom, dateTo);
    }

    @GetMapping(value="ordersWithoutInvoice/{year}")
    public List<OrderDTO> getAllOrdersWithoutInvoice(@PathVariable int year) {
        LocalDate dateFrom = LocalDate.of(year, 1, 1);
        LocalDate dateTo = LocalDate.of(year, 12, 31);

        return accountingGasService.getOrdersWithoutInvoice(dateFrom, dateTo, false);
    }

    @GetMapping(value="syncAequosOrdersWithoutInvoice/{year}")
    public BasicResponseDTO syncAequosOrdersWithoutInvoice(@PathVariable int year) {
        LocalDate dateFrom = LocalDate.of(year, 1, 1);
        LocalDate dateTo = LocalDate.of(year, 12, 31);

        List<OrderDTO> aequosOrdersWithoutInvoice = accountingGasService.getOrdersWithoutInvoice(dateFrom, dateTo, true);

        long orderUpdated = aequosOrdersWithoutInvoice.stream()
                .map(this::synchOrderWithAequos)
                .filter(Boolean::booleanValue)
                .count();

        return new BasicResponseDTO(orderUpdated);
    }

    private boolean synchOrderWithAequos(OrderDTO aequosOrder) {
        try {
            return orderManagerService.synchOrderWithAequos(aequosOrder.getId(), true);
        } catch (GoGasException e) {
            log.error("Error while synchronizing order {} with Aequos", aequosOrder.getId());
            return false;
        } catch (Exception ex) {
            log.error("Error while synchronizing order {} with Aequos, generic exception ({})", aequosOrder.getId(), ex.getMessage());
            return false;
        }
    }
}
