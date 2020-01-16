package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.MenuDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.service.MenuService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/home")
public class HomeController {

    private UserRepo userRepo;
    private MenuService menuService;
    private PushNotificationSender pushNotificationSender;

    public HomeController(UserRepo userRepo, MenuService menuService, PushNotificationSender pushNotificationSender) {
        this.userRepo = userRepo;
        this.menuService = menuService;
        this.pushNotificationSender = pushNotificationSender;
    }

    @GetMapping(value = "balance", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BigDecimal getBalance(@RequestParam String userId) {
        return userRepo.getBalance(userId);
    }

    @GetMapping(value = "menu", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<MenuDTO> getMenu(@RequestParam String role) {
        return menuService.getMenuTreeByRole(role);
    }

    //TODO: move to appropriate controller and put logic in service
    @GetMapping(value = "order/status")
    public List<SelectItemDTO> getOrderStatusList() {
        return Arrays.stream(Order.OrderStatus.values())
                .map(s -> new SelectItemDTO(Integer.toString(s.getStatusCode()), s.getDescription()))
                .collect(Collectors.toList());
    }
}
