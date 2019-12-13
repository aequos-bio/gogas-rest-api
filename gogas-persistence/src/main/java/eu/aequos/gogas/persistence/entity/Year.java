package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "anno")
public class Year {
    @Id
    @Column(name = "anno" , columnDefinition="uniqueidentifier")
    private int year;

    @Column(name = "chiuso")
    private boolean closed;

}
