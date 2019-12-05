package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.MenuDTO;
import eu.aequos.gogas.persistence.entity.UserSummary;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.service.MenuService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/home")
public class HomeController {

    private UserRepo userRepo;
    private MenuService menuService;
    private OrderRepo orderSummaryRepo;

    public HomeController(UserRepo userRepo, MenuService menuService, OrderRepo orderSummaryRepo) {
        this.userRepo = userRepo;
        this.menuService = menuService;
        this.orderSummaryRepo = orderSummaryRepo;
    }

    @GetMapping(value = "friends", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<UserSummary> getFriends(@RequestParam String userId) {
        return new ArrayList<>(); //userRepo.findByFriendReferralId(userId);
    }

    @GetMapping(value = "balance", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BigDecimal getBalance(@RequestParam String userId) {
        return userRepo.getBalance(userId);
    }

    @GetMapping(value = "menu", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<MenuDTO> getMenu(@RequestParam String role) {
        return menuService.getMenuTreeByRole(role);
    }
}
