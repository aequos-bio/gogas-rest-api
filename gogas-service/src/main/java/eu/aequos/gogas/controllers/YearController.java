package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.persistence.entity.Year;
import eu.aequos.gogas.persistence.repository.YearRepo;
import eu.aequos.gogas.utils.RestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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

    @PutMapping(value = "/close/{year}")
    public BasicResponseDTO close(@PathVariable int year) {
        Optional<Year> opt = repo.findById(year);
        if (opt.isPresent()) {
            Year y = opt.get();
            y.setClosed(true);
            repo.save(y);
        }
        return new BasicResponseDTO(year);
    }
}
