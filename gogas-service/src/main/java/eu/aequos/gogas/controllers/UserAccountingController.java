package eu.aequos.gogas.controllers;

import java.math.BigDecimal;
import java.util.*;

import eu.aequos.gogas.utils.ExcelExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserAccountingRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.persistence.utils.UserTotal;
import eu.aequos.gogas.persistence.utils.UserTotalProjection;
import eu.aequos.gogas.persistence.utils.UserTransactionFull;
import eu.aequos.gogas.persistence.utils.UserTransactionFullProjection;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsAdminOrCurrentUser;
import eu.aequos.gogas.service.UserAccountingService;
import eu.aequos.gogas.utils.RestResponse;

import javax.servlet.http.HttpServletResponse;

//TODO: check if we can merge with other controllers
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
    private ExcelExport excelExport;

    @IsAdmin
    @GetMapping("/userTotals")
    public @ResponseBody
    RestResponse<List<UserTotal>> getUserTotals() {
        return new RestResponse<>(_getUserTotals());
    }

    public List<UserTotal> _getUserTotals() {
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

        return ttt;
    }

    @IsAdminOrCurrentUser
    @GetMapping("/userTransactions")
    public @ResponseBody
    RestResponse<List<UserTransactionFullProjection>> getUserTransactions(@RequestParam(name = "userId", required = true) String userId) {
        List<UserTransactionFull> movimenti = userAccountingSrv.getUserTransactions(userId);
        List<UserTransactionFullProjection> ordini = userAccountingRepo.getUserRecordedOrders(userId, userId);
        ordini.addAll(movimenti);
        return new RestResponse<>(ordini);
    }

    //@IsAdmin
    @GetMapping(value = "/exportUserTotals", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody
    byte[] exportUserTotals(HttpServletResponse response,
                            @RequestParam(name = "includeUsers", required = false) Boolean includeUsers) throws Exception {

        List<UserTotal> userTotals = _getUserTotals();
        Collections.sort(userTotals, new Comparator<UserTotal>() {
            @Override
            public int compare(UserTotal o1, UserTotal o2) {
                int a1 = o1.getUser().isEnabled() ? 1 : 0;
                int a2 = o2.getUser().isEnabled() ? 1 : 0;
                return a2 - a1;
            }
        });
        return excelExport.exportUserTotals(userTotals, includeUsers == null ? false : includeUsers.booleanValue());

    }

    //@IsAdminOrCurrentUser
    @GetMapping(value = "/exportUserDetails", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public @ResponseBody
    byte[] exportUserDetails(HttpServletResponse response,
                             @RequestParam(name = "userId", required = true) String userId) throws Exception {
        byte[] bytes = excelExport.exportUserDetails(userId);
        if (bytes == null)
            response.sendError(406);
        return bytes;
    }
}