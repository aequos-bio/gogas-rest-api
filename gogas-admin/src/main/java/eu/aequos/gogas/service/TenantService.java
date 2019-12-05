package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.TenantDTO;
import eu.aequos.gogas.exception.DeploymentException;
import eu.aequos.gogas.persistence.entity.Tenant;
import eu.aequos.gogas.persistence.repository.TenantRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private TenantRepo tenantRepo;
    private DeploymentService deploymentService;

    public TenantService(TenantRepo tenantRepo, DeploymentService deploymentService) {
        this.tenantRepo = tenantRepo;
        this.deploymentService = deploymentService;
    }

    public List<TenantDTO> getTenants() {
        return tenantRepo.findAll().stream()
                .map(t -> new TenantDTO(t.getTenantId(), t.getGasName(), t.getIisSite()))
                .collect(Collectors.toList());
    }

    public long createTenant(TenantDTO tenant) throws DeploymentException {
        //todo: check already exists
        deploymentService.deployInstance(tenant.getTenantId(), tenant.getIisSite());
        return createDbEntity(tenant);
    }

    private long createDbEntity(TenantDTO tenant) {
        Tenant tenantEntity = new Tenant();
        tenantEntity.setTenantId(tenant.getTenantId());
        tenantEntity.setGasName(tenant.getGasName());
        tenantEntity.setIisSite(tenant.getIisSite());
        tenantEntity.setJdbcUrl("");
        tenantEntity.setUsername(tenant.getTenantId() + "_user");
        tenantEntity.setPassword(tenant.getTenantId());

        return tenantRepo.save(tenantEntity)
                .getId();
    }
}
