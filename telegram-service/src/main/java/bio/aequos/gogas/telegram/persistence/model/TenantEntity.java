package bio.aequos.gogas.telegram.persistence.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private String id;
}
