package ufms.facoffe.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import ufms.facoffe.finance.domain.enums.PaymentMethod;
import ufms.facoffe.finance.domain.enums.PaymentProofStatus;

@Entity
@Table(name = "payment_proofs")
@Data
public class PaymentProof {
    @Id
    private Long id;
    private Long pendingId;
    private String userId;
    private BigDecimal amount;
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String receiptUrl;


    @Enumerated(EnumType.STRING)
    private PaymentProofStatus status;

    private String note;
    private LocalDateTime submittedAt;
    
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}
