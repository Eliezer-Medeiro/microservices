package ufms.facoffe.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ufms.facoffe.finance.domain.Expense;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.domain.PaymentProof;
import ufms.facoffe.finance.domain.enums.ExpenseCategory;
import ufms.facoffe.finance.domain.enums.PaymentProofStatus;
import ufms.facoffe.finance.domain.enums.PendencyStatus;
import ufms.facoffe.finance.messaging.FinanceEventProducer;
import ufms.facoffe.finance.repository.ExpenseRepository;
import ufms.facoffe.finance.repository.PaymentProofRepository;
import ufms.facoffe.finance.repository.PendencyRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
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

    @Autowired
    private FinanceEventProducer financeEventProducer;

    // ==========================================
    // PENDÊNCIAS (PENDENCIES)
    // ==========================================

    public Page<FinancialPending> listPendencies(String userId, String cycle, PendencyStatus status, Pageable pageable) {
        return pendencyRepository.findWithFilters(userId, cycle, status, pageable);
    }

    public Optional<FinancialPending> getPendencyById(String id) {
        return pendencyRepository.findById(id);
    }

    @Transactional
    public FinancialPending createPendency(FinancialPending pendency) {
        pendency.setStatus(PendencyStatus.PENDING);
        pendency.setCreatedAt(LocalDateTime.now());
        
        FinancialPending savedPendency = pendencyRepository.save(pendency);
        financeEventProducer.publishPendencyCreated(savedPendency);
        
        return savedPendency;
    }

    // ==========================================
    // COMPROVANTES (PROOFS)
    // ==========================================

    @Transactional
    public PaymentProof submitPaymentProof(String pendencyId, PaymentProof proof) {
        FinancialPending pendency = pendencyRepository.findById(pendencyId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência financeira não encontrada com o ID: " + pendencyId));

        proof.setPendingId(pendencyId);
        proof.setStatus(PaymentProofStatus.WAITING_APPROVAL); // Alinhado com o enum do seu projeto
        proof.setSubmittedAt(LocalDateTime.now());

        // Altera o estado da pendência conforme a regra de negócio do contrato
        pendency.setStatus(PendencyStatus.WAITING_VALIDATION); 
        pendencyRepository.save(pendency);

        return paymentProofRepository.save(proof);
    }

    public Page<PaymentProof> listPendencyPaymentProofs(String pendencyId, PaymentProofStatus status, Pageable pageable) {
        // Certifique-se de ter esse método derivado ou query no seu PaymentProofRepository
        if (status != null) {
            return paymentProofRepository.findByPendingIdAndStatus(pendencyId, status, pageable);
        }
        return paymentProofRepository.findByPendingId(pendencyId, pageable);
    }

    @Transactional
    public PaymentProof decidePaymentProof(String pendencyId, String proofId, PaymentProofStatus status, String reason, String decidedBy) {
        PaymentProof proof = paymentProofRepository.findById(proofId)
                .orElseThrow(() -> new IllegalArgumentException("Comprovante não encontrado com o ID: " + proofId));

        // Validação do estado do comprovante de acordo com a regra OpenAPI
        if (proof.getStatus() != PaymentProofStatus.WAITING_APPROVAL) { 
            throw new IllegalStateException("O comprovante não pode ser decidido no estado atual.");
        }

        FinancialPending pendency = pendencyRepository.findById(pendencyId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência associada não encontrada."));

        proof.setStatus(status);

        if (status == PaymentProofStatus.VALIDATED) {
            proof.setValidatedBy(decidedBy);
            proof.setValidatedAt(LocalDateTime.now());
            
            pendency.setStatus(PendencyStatus.PAID);
            pendency.setPaidAt(LocalDateTime.now());
        } else if (status == PaymentProofStatus.REJECTED) {
            proof.setRejectedBy(decidedBy);
            proof.setRejectedAt(LocalDateTime.now());
            proof.setRejectionReason(reason);
            
            pendency.setStatus(PendencyStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Status de decisão inválido.");
        }

        pendencyRepository.save(pendency);
        return paymentProofRepository.save(proof);
    }

    @Transactional
    public void deletePaymentProof(String pendencyId, String proofId) {
        PaymentProof proof = paymentProofRepository.findById(proofId)
                .orElseThrow(() -> new IllegalArgumentException("Comprovante não encontrado."));

        if (proof.getStatus() != PaymentProofStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("Não é possível remover comprovante que já foi analisado.");
        }

        paymentProofRepository.delete(proof);

        // Opcional: Retornar o status da pendência para PENDING caso não existam outros comprovantes
        FinancialPending pendency = pendencyRepository.findById(pendencyId).orElse(null);
        if (pendency != null) {
            pendency.setStatus(PendencyStatus.PENDING);
            pendencyRepository.save(pendency);
        }
    }

    // ==========================================
    // DESPESAS (EXPENSES)
    // ==========================================

    @Transactional
    public Expense createExpense(Expense expense) {
        if (expense.getAmount() == null || expense.getAmount().doubleValue() <= 0) {
            throw new IllegalArgumentException("O valor da despesa deve ser maior que zero.");
        }
        return expenseRepository.save(expense);
    }

    public Page<Expense> listExpenses(String cycle, ExpenseCategory category, Pageable pageable) {
        // Certifique-se de implementar os filtros correspondentes no seu ExpenseRepository se necessário
        return expenseRepository.findAll(pageable);
    }

    public Optional<Expense> getExpenseById(String expenseId) {
        return expenseRepository.findById(expenseId);
    }

    @Transactional
    public void deleteExpense(String expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada com o ID: " + expenseId));
        expenseRepository.delete(expense);
    }

    // ==========================================
    // RELATÓRIOS E EVENTOS
    // ==========================================

    public Object getFinanceStatement(LocalDate startDate, LocalDate endDate) {
        // Implementar lógica que calcula entradas/saídas baseando-se em datas futuras
        log.info("Buscando extrato de {} até {}", startDate, endDate);
        return null; // Retorne seu DTO estruturado aqui
    }

    public Object getFinanceBalance() {
        // Implementar cálculo de todo o histórico financeiro acumulado
        log.info("Buscando balanço financeiro consolidado");
        return null; // Retorne seu DTO estruturado aqui
    }

    @Transactional
    public void handleUserDeactivation(String userId, String eventId) {
        log.info("A cancelar pendências em aberto para o utilizador desativado: {}", userId);
        
        List<FinancialPending> activePendencies = pendencyRepository.findAll()
                .stream()
                .filter(p -> p.getUserId().equals(userId) && 
                            (p.getStatus() == PendencyStatus.PENDING || p.getStatus() == PendencyStatus.WAITING_VALIDATION))
                .toList();

        for (FinancialPending pendency : activePendencies) {
            pendency.setStatus(PendencyStatus.CANCELLED);
            pendencyRepository.save(pendency);
        }
    }
}