package eu.aequos.gogas.controllers;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserAccountingRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.persistence.utils.UserTotal;
import eu.aequos.gogas.persistence.utils.UserTotalProjection;
import eu.aequos.gogas.persistence.utils.UserTransactionFull;
import eu.aequos.gogas.security.annotations.CanViewBalance;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsAdminOrCurrentUser;
import eu.aequos.gogas.service.ExcelGenerationService;
import eu.aequos.gogas.service.UserAccountingService;
import eu.aequos.gogas.utils.RestResponse;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private UserAccountingService userAccountingSrv;

    @Autowired
    private ExcelGenerationService excelGenerationService;

    //TODO: use database aggregation
    @IsAdmin
    @GetMapping("/userTotals")
    public RestResponse<List<UserTotal>> getUserTotals() {
        List<UserTotalProjection> order = userAccountingRepo.findOrderTotals();
        List<UserTotalProjection> transaction = userAccountingRepo.findTransactionTotals();

        Map<String, UserTotal> totals = new HashMap<>();
        List<User> users = userRepo.findAll();
        for (User u : users)
            if (u.getFriendReferral() == null)
                totals.put(u.getId(), new UserTotal(u, BigDecimal.ZERO));

        for (UserTotalProjection t : order) {
            if (totals.containsKey(t.getUserId())) {
                UserTotal tot = totals.get(t.getUserId());
                tot.setTotal(tot.getTotal().subtract(t.getTotal()));
            }
        }

        for (UserTotalProjection t : transaction) {
            if (totals.containsKey(t.getUserId())) {
                UserTotal tot = totals.get(t.getUserId());
                tot.setTotal(tot.getTotal().add(t.getTotal()));
            }
        }

        List<UserTotal> ttt = new ArrayList<>();
        for (User u : users)
            if (u.getFriendReferral() == null)
                ttt.add(totals.get(u.getId()));

        return new RestResponse<>(ttt);
    }

    @CanViewBalance
    @GetMapping("/userTransactions")
    public RestResponse<List<UserTransactionFull>> getUserTransactions(@RequestParam(name = "userId") String userId) {
        List<UserTransactionFull> movimenti = userAccountingSrv.getUserTransactions(userId);
        List<UserTransactionFull> ordini = userAccountingRepo.getUserRecordedOrders(userId, userId);
        ordini.addAll(movimenti);
        return new RestResponse<>(ordini);
    }

    @IsAdmin
    @GetMapping(value = "/exportUserTotals", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserTotals(HttpServletResponse response,
                            @RequestParam(name = "includeUsers", required = false) boolean includeUsers) throws Exception {

        return excelGenerationService.exportUserTotals(includeUsers);
    }


    @IsAdminOrCurrentUser
    @GetMapping(value = "/exportUserDetails", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody byte[] exportUserDetails(HttpServletResponse response, @RequestParam(name = "userId") String userId) throws Exception {
        byte[] bytes = excelGenerationService.exportUserEntries(userId);
        if (bytes == null)
            response.sendError(406);
        return bytes;
    }
}