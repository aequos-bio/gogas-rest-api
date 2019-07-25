package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Data
@Embeddable
public class MenuByRoleId implements Serializable {

    @ManyToOne
    @JoinColumn(name = "idmenu", nullable = false, columnDefinition="uniqueidentifier")
    private Menu menu;

    @Column(name = "ruolo", nullable = false, length = 10)
    private String role;
}
