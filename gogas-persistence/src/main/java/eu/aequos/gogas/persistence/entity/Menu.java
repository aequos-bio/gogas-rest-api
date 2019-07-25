package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "menu")
public class Menu {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idmenu" , columnDefinition="uniqueidentifier")
    private String id;

    @Column(name = "label", nullable = false, length = 50)
    private String label;

    @Column(name = "url")
    private String url;

    @Column(name = "parentmenu" , columnDefinition="uniqueidentifier")
    private String parentMenuId;

    @Column(name = "external")
    private boolean external;
}
