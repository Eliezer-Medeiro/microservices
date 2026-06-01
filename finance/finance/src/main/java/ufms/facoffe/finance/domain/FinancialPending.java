package ufms.facoffe.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import ufms.facoffe.finance.domain.enums.PendencyStatus;

@Entity
@Table(name = "financial_pending")
@Data
public class FinancialPending {
    @Id
    private Long id;
    private String source;
    private String sourceId;
    private String userId;
    private String cycle;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PendencyStatus status;

    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

}
