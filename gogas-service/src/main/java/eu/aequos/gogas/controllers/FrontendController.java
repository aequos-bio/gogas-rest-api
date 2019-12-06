package eu.aequos.gogas.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping(value = {"/", "/login", "/useraccounting", "/useraccountingdetails", "/users"})
    public String home() {
        return "singlepage";
    }
}
