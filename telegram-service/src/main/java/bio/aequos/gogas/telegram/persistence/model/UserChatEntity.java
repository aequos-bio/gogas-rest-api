package bio.aequos.gogas.telegram.persistence.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "userchat")
public class UserChatEntity {

    @Id
    @Column(name = "chatid", nullable = false)
    private long chatId;

    @Column(name = "tenantid", nullable = false)
    private String tenantId;

    @Column(name = "userid" , columnDefinition = "uniqueidentifier", nullable = false)
    private String userId;

    @Column(name = "datecreated", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "lastupdate", nullable = false)
    private LocalDateTime lastUpdate;
}
