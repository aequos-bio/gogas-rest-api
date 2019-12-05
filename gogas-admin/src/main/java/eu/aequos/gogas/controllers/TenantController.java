package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.TenantDTO;
import eu.aequos.gogas.exception.DeploymentException;
import eu.aequos.gogas.service.TenantService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("tenant")
public class TenantController {

    private TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<TenantDTO> getTenants() {
        return tenantService.getTenants();
    }

    @PostMapping( produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void createTenant(@RequestBody TenantDTO tenant) throws DeploymentException {
        tenantService.createTenant(tenant);
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void updateTenant(@RequestParam TenantDTO tenant) {

    }
}
