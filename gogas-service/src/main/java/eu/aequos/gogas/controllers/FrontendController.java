package eu.aequos.gogas.controllers;

import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsManager;
import eu.aequos.gogas.security.annotations.IsOrderManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FrontendController {

    @GetMapping(value = {
            "/", "/login",
            "/years", "/useraccounting", "/useraccountingdetails", "/gasaccounting", "/invoices",
            "/users", "/reasons", "/ordertypes", "/accountingcodes", "/managers",
            "/legacy/orderslist", "/legacy/products", "/legacy/suppliers", "/legacy/configuration",
            "/legacy/ordershistory",
    })
    public String home() {
        return "singlepage";
    }

    @IsManager
    @GetMapping(value = {"/legacy-ui/orders-list"})
    public String legacyOrdersList() {
        return "legacy/referenti/orders-list";
    }

    @IsOrderManager
    @GetMapping(value = {"/legacy-ui/order-details"})
    public String legacyOrderDetails(@RequestParam String orderId,
                                     Model model) {
        model.addAttribute("orderId", orderId);
        return "legacy/referenti/order-details";
    }

    @IsOrderManager
    @GetMapping(value = {"/legacy-ui/order-details-byuser"})
    public String legacyOrderDetailsByUser(@RequestParam String orderId,
                                           Model model) {
        model.addAttribute("orderId", orderId);
        return "legacy/referenti/order-details-byuser";
    }

    @IsManager
    @GetMapping(value = {"/legacy-ui/products"})
    public String legacyProducts() {
        return "legacy/admin/products";
    }

    @IsManager
    @GetMapping(value = {"/legacy-ui/orders-report"})
    public String legacyOrdersReport() {
        return "legacy/referenti/orders-report";
    }

    @IsManager
    @GetMapping(value = {"/legacy-ui/suppliers"})
    public String legacySuppliers() {
        return "legacy/admin/suppliers";
    }

    @IsAdmin
    @GetMapping(value = {"/legacy-ui/configuration"})
    public String legacyConfiguration() {
        return "legacy/admin/configuration";
    }

    @GetMapping(value = {"/legacy-ui/user-orders-list"})
    public String legacyUserOrdersList() {
        return "legacy/user/orders-list";
    }

    @GetMapping(value = {"/legacy-ui/user-order-details"})
    public String legacyUserOrderDetails(@RequestParam String orderId, @RequestParam String userId,
                                         Model model) {

        model.addAttribute("orderId", orderId);
        model.addAttribute("userId", userId);
        return "legacy/user/order-details";
    }

    @GetMapping(value = {"/legacy-ui/friend-order-details"})
    public String legacyFriendOrderDetails(@RequestParam String orderId, Model model) {

        model.addAttribute("orderId", orderId);
        return "legacy/user/friend-order";
    }

    @GetMapping(value = {"/legacy-ui/manage-friends"})
    public String legacyManageFriends() {
        return "legacy/user/manage-friends";
    }
}
