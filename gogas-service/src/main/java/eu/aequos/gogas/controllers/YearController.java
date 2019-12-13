package eu.aequos.gogas.controllers;

import eu.aequos.gogas.persistence.entity.Year;
import eu.aequos.gogas.persistence.repository.YearRepo;
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
        List<Year> openyears = repo.findYearsByClosedFalseOrderByYearAsc();
        return openyears==null || openyears.isEmpty() ? null : openyears.get(0);
    }
}
