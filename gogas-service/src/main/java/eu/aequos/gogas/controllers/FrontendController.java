package eu.aequos.gogas.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FrontendController {

    @GetMapping(value = {"/", "/login",
        "/years", "/useraccounting", "/useraccountingdetails", "/gasaccounting", "/invoices",
        "/users", "/reasons", "/ordertypes", "/accountingcodes", "/managers"})
    public String home() {
        return "singlepage";
    }

    @GetMapping(value = {"/legacy/orders-list"})
    public String legacyOrdersList() {
        return "legacy/referenti/orders-list";
    }

    @GetMapping(value = {"/legacy/order-details"})
    public String legacyOrderDetails(@RequestParam String orderId,
                                     Model model) {
        model.addAttribute("orderId", orderId);
        return "legacy/referenti/order-details";
    }

    @GetMapping(value = {"/legacy/order-details-byuser"})
    public String legacyOrderDetailsByUser(@RequestParam String orderId,
                                           Model model) {
        model.addAttribute("orderId", orderId);
        return "legacy/referenti/order-details-byuser";
    }
}
