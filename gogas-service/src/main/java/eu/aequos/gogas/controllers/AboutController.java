package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AboutDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("about")
public class AboutController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public AboutDTO about() {
        return new AboutDTO()
                .setVersion("1.0.0") //TODO: read from POM
                .setName("gogas-api")
                .setDescription("GoGAs REST APIs")
                .setCopyrights("Cooperativa Aequos")
                .setAuthors(Arrays.asList("Davide Lorusso", "Ermanno Scanagatta"));
    }
}
