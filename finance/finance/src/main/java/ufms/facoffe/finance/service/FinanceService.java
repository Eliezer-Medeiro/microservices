package ufms.facoffe.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ufms.facoffe.finance.domain.Expense;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.domain.PaymentProof;
import ufms.facoffe.finance.domain.enums.PaymentProofStatus;
import ufms.facoffe.finance.domain.enums.PendencyStatus;
import ufms.facoffe.finance.messaging.FinanceEventProducer;
import ufms.facoffe.finance.repository.ExpenseRepository;
import ufms.facoffe.finance.repository.PaymentProofRepository;
import ufms.facoffe.finance.repository.PendencyRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FinanceService {

    @Autowired
    private PendencyRepository pendencyRepository;

    @Autowired
    private PaymentProofRepository paymentProofRepository;

    @Autowired
    private ExpenseRepository expenseRepository;


    public Page<FinancialPending> listPendencies(String userId, String cycle, PendencyStatus status, Pageable pageable) {
        return pendencyRepository.findWithFilters(userId, cycle, status, pageable);
    }

    public Optional<FinancialPending> getPendencyById(Long id) {
        return pendencyRepository.findById(id);
    }


    @Transactional
    public PaymentProof submitPaymentProof(Long pendencyId, PaymentProof proof) {
        FinancialPending pendency = pendencyRepository.findById(pendencyId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência financeira não encontrada com o ID: " + pendencyId));

        proof.setPendingId(pendencyId);
        proof.setStatus(PaymentProofStatus.WAITING_APPROVAL);
        proof.setSubmittedAt(LocalDateTime.now());

        pendency.setStatus(PendencyStatus.PENDING);
        pendencyRepository.save(pendency);

        return paymentProofRepository.save(proof);
    }

    @Transactional
    public PaymentProof reviewPaymentProof(Long proofId, PaymentProofStatus newStatus, String rejectionReason, String managerId) {
        PaymentProof proof = paymentProofRepository.findById(proofId)
                .orElseThrow(() -> new IllegalArgumentException("Comprovante não encontrado com o ID: " + proofId));

        if (proof.getStatus() != PaymentProofStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("Este comprovante já foi processado e possui o status: " + proof.getStatus());
        }

        Long pendencyId = Long.valueOf(proof.getPendingId());
        FinancialPending pendency = pendencyRepository.findById(pendencyId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência associada ao comprovante não foi encontrada."));

        proof.setStatus(newStatus);

        if (newStatus == PaymentProofStatus.VALIDATED) {
            proof.setValidatedBy(managerId);
            proof.setValidatedAt(LocalDateTime.now());
            
            pendency.setStatus(PendencyStatus.PAID);
            pendency.setPaidAt(LocalDateTime.now());
        } else if (newStatus == PaymentProofStatus.REJECTED) {
            proof.setRejectedBy(managerId);
            proof.setRejectedAt(LocalDateTime.now());
            proof.setRejectionReason(rejectionReason);
            
            pendency.setStatus(PendencyStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Status de revisão inválido. Use VALIDATED ou REJECTED.");
        }

        pendencyRepository.save(pendency);
        return paymentProofRepository.save(proof);
    }


    @Transactional
    public Expense createExpense(Expense expense) {
        if (expense.getAmount() == null || expense.getAmount().doubleValue() <= 0) {
            throw new IllegalArgumentException("O valor da despesa deve ser maior que zero.");
        }
        return expenseRepository.save(expense);
    }

    public List<Expense> listExpenses() {
        return expenseRepository.findAll();
    }

    @Autowired
    private FinanceEventProducer financeEventProducer;

    @Transactional
    public FinancialPending createPendency(FinancialPending pendency) {
        pendency.setStatus(PendencyStatus.PENDING);
        pendency.setCreatedAt(LocalDateTime.now());
        
        FinancialPending savedPendency = pendencyRepository.save(pendency);
        
        financeEventProducer.publishPendencyCreated(savedPendency);
        
        return savedPendency;
    }

    @Transactional
    public void handleUserDeactivation(String userId, String eventId) {
      
        log.info("A cancelar pendências em aberto para o utilizador desativado: {}", userId);
        
        List<FinancialPending> activePendencies = pendencyRepository.findAll() // Idealmente filtrado via JPQL por userId e status ativo
                .stream()

                .filter(p -> p.getUserId().equals(userId) && 
                            (p.getStatus() == PendencyStatus.PENDING || p.getStatus() == PendencyStatus.PENDING))
                .toList();

        for (FinancialPending pendency : activePendencies) {
            pendency.setStatus(PendencyStatus.CANCELLED); // Compatível com PendencyStatus.java
            pendencyRepository.save(pendency);
        }
    }
}