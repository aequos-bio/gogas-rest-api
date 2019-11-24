package eu.aequos.gogas.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@RestController
@RequestMapping(value = "api/useraccounting")
public class UserAccountingController {
  
  @Autowired
  private UserRepo userRepo;
  @Autowired
  private UserAccountingRepo userAccountingRepo;
  @Autowired
  private UserAccountingService userAccountingSrv;

  @GetMapping("/userTotals")
  @IsAdmin
  public @ResponseBody RestResponse<Collection<UserTotal>> getUserTotals() {
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

  @GetMapping("/userTransactions")
  @IsAdminOrCurrentUser
  public @ResponseBody
  RestResponse<List<UserTransactionFull>> getUserTransactions(@RequestParam(name = "userId", required = true) String userId) {
      List<UserTransactionFull> movimenti = userAccountingSrv.getUserTransactions(userId);
      List<UserTransactionFullProjection> ordini = userAccountingRepo.getUserRecordedOrders(userId);
      ordini.addAll(movimenti);
      return new RestResponse<>(movimenti);
  }

}