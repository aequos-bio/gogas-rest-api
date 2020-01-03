package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "pushtoken")
public class PushToken {

    @Id
    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "idutente", nullable = false)
    private String userId;

    @Column(name = "deviceid")
    private String deviceId;
}