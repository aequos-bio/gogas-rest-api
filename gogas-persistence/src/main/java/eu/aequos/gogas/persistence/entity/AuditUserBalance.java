package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audituserbalance")
public class AuditUserBalance {

    public enum EntryType {
        ORDER, ACCOUNTING
    }

    public enum OperationType {
        ADD, UPDATE, REMOVE
    }

    @Id
    @Column(name = "id" , columnDefinition = "uniqueidentifier", nullable = false)
    private String id;

    @Column(name = "userid" , columnDefinition = "uniqueidentifier", nullable = false)
    private String userId;

    @Column(name = "ts", nullable = false)
    private LocalDateTime ts;

    @Column(name = "entrytype", nullable = false)
    private EntryType entryType;

    @Column(name = "operationtype", nullable = false)
    private OperationType operationType;

    @Column(name = "referenceid", columnDefinition = "uniqueidentifier")
    private String referenceId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 5)
    private BigDecimal amount;

    @Column(name = "currentbalance", nullable = false, precision = 18, scale = 5)
    private BigDecimal currentBalance;
}
