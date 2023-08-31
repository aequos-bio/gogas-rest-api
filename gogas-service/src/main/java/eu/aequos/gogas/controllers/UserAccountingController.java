package eu.aequos.gogas.controllers;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserAccountingRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.persistence.utils.UserTotal;
import eu.aequos.gogas.persistence.utils.UserTransactionFull;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsAdminOrCurrentUser;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ExcelGenerationService;
import eu.aequos.gogas.utils.RestResponse;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

//TODO: check if we can merge with other controllers
@Api("Reports for user accounting")
@RestController
@RequestMapping(value = "api/useraccounting")
public class UserAccountingController {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private UserAccountingRepo userAccountingRepo;
    @Autowired
    private AccountingService accountingService;

    @Autowired
    private ExcelGenerationService excelGenerationService;

    @IsAdmin
    @GetMapping("/userTotals")
    public RestResponse<List<UserTotal>> getUserTotals() {
        List<UserTotal> userTotals = userRepo.findByRole(User.Role.U.name()).stream()
                .map(UserTotal::new).collect(Collectors.toList());

        return new RestResponse<>(userTotals);
    }

    @IsAdminOrCurrentUser
    @GetMapping("/userTransactions")
    public RestResponse<List<UserTransactionFull>> getUserTransactions(@RequestParam(name = "userId") String userId) {
        List<UserTransactionFull> allAccountingEntries = accountingService.getAllEntriesByUser(userId);
        return new RestResponse<>(allAccountingEntries);
    }

    @IsAdmin
    @GetMapping(value = "/exportUserTotals", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserTotals(HttpServletResponse response,
                            @RequestParam(name = "includeUsers", required = false) boolean includeUsers) throws IOException {

        return excelGenerationService.exportUserTotals(includeUsers);
    }


    @IsAdminOrCurrentUser
    @GetMapping(value = "/exportUserDetails", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserDetails(HttpServletResponse response, @RequestParam(name = "userId") String userId) throws IOException {
        byte[] bytes = excelGenerationService.exportUserEntries(userId);
        if (bytes == null)
            response.sendError(406);
        return bytes;
    }
}