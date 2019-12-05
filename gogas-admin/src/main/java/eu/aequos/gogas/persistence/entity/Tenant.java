package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "master_tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "username")
    private String username;

    @JoinColumn(name = "password")
    private String password;

    @Column(name = "url")
    private String jdbcUrl;

    @Column(name = "gas_name")
    private String gasName;

    @Column(name = "iis_site")
    private String iisSite;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
