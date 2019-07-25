package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "menuruolo")
public class MenuByRole {

    @EmbeddedId
    private MenuByRoleId id;

    @Column(name = "ordine", nullable = false)
    private int order;

    public Menu getMenu() {
        return id != null ? id.getMenu() : null;
    }
}
