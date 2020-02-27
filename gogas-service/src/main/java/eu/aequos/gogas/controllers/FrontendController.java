package eu.aequos.gogas.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping(value = {"/", "/login",
        "/years", "/useraccounting", "/useraccountingdetails",
        "/users", "/reasons", "/ordertypes", "/managers"})
    public String home() {
        return "singlepage";
    }
}
