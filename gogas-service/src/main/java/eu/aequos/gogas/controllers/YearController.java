package eu.aequos.gogas.controllers;

import eu.aequos.gogas.persistence.entity.Year;
import eu.aequos.gogas.persistence.repository.YearRepo;
import eu.aequos.gogas.utils.RestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/year")
public class YearController {
    @Autowired
    private YearRepo repo;

    @GetMapping("/current")
    public @ResponseBody Year getOpenYear() {
        List<Year> openyears = repo.findYearsByClosedFalseOrderByYearDesc();
        return openyears==null || openyears.isEmpty() ? null : openyears.get(0);
    }

    @GetMapping("/all")
    public @ResponseBody
    RestResponse<List<Year>> getAllYears() {
        List<Year> years = repo.findAll();
        return new RestResponse<>(years);
    }
}
