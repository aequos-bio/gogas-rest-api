package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.MenuDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.multitenancy.TenantContext;
import eu.aequos.gogas.notification.telegram.client.TelegramActivationDTO;
import eu.aequos.gogas.notification.telegram.client.TelegramNotificationClient;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import eu.aequos.gogas.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/home")
public class HomeController {

    private final UserRepo userRepo;
    private final MenuService menuService;
    private final AuthorizationService authorizationService;
    private final TelegramNotificationClient telegramNotificationClient;

    @GetMapping(value = "balance", produces = MediaType.APPLICATION_JSON_VALUE)
    public BigDecimal getBalance(@RequestParam String userId) {
        return userRepo.getBalance(userId);
    }

    @GetMapping(value = "menu", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping(value = "telegram/activation", produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicResponseDTO generateTelegramToken() {
        String tenantId = TenantContext.getTenantId()
                .orElseThrow(() -> new GoGasException("Invalid tenant"));

        GoGasUserDetails currentUser = authorizationService.getCurrentUser();

        TelegramActivationDTO telegramActivationDTO = telegramNotificationClient.activateUser(tenantId, currentUser.getId());
        return new BasicResponseDTO("GoGasAppBot?start=" + telegramActivationDTO.getCode());
    }
}
