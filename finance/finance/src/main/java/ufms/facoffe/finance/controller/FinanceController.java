package ufms.facoffe.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ufms.facoffe.finance.domain.Expense;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.domain.PaymentProof;
import ufms.facoffe.finance.domain.enums.ExpenseCategory;
import ufms.facoffe.finance.domain.enums.PaymentProofStatus;
import ufms.facoffe.finance.domain.enums.PendencyStatus;
import ufms.facoffe.finance.service.FinanceService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    
    // CLASSES AUXILIARES (DTOs)
    
    public static class PaymentProofDecisionRequest {
        private PaymentProofStatus status;
        private String reason;
        private String decidedBy;

        public PaymentProofStatus getStatus() { return status; }
        public void setStatus(PaymentProofStatus status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getDecidedBy() { return decidedBy; }
        public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    }

    
    // PENDÊNCIAS
    

    @GetMapping("/pendencies")
    public ResponseEntity<Page<FinancialPending>> listPendencies(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String cycle,
            @RequestParam(required = false) PendencyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<FinancialPending> pendencies = financeService.listPendencies(userId, cycle, status, pageable);
        return ResponseEntity.ok(pendencies);
    }

    @GetMapping("/pendencies/{pendencyId}")
    public ResponseEntity<FinancialPending> getPendencyById(@PathVariable String pendencyId) {
        return financeService.getPendencyById(pendencyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    
    // COMPROVANTES (PROOFS)
    

    @PostMapping("/pendencies/{pendencyId}/proofs")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<PaymentProof> submitPaymentProof(
            @PathVariable String pendencyId, 
            @RequestBody PaymentProof proof) { 
        
        PaymentProof savedProof = financeService.submitPaymentProof(pendencyId, proof);
        return ResponseEntity.status(201).body(savedProof);
    }

    @GetMapping("/pendencies/{pendencyId}/proofs")
    public ResponseEntity<Page<PaymentProof>> listPendencyPaymentProofs(
            @PathVariable String pendencyId,
            @RequestParam(required = false) PaymentProofStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentProof> proofs = financeService.listPendencyPaymentProofs(pendencyId, status, pageable);
        return ResponseEntity.ok(proofs);
    }

    @PatchMapping("/pendencies/{pendencyId}/proofs/{proofId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PaymentProof> decidePaymentProof(
            @PathVariable String pendencyId,
            @PathVariable String proofId,
            @RequestBody PaymentProofDecisionRequest request) { 
        
        PaymentProof updatedProof = financeService.decidePaymentProof(
                pendencyId, 
                proofId, 
                request.getStatus(), 
                request.getReason(), 
                request.getDecidedBy()
        );
        
        return ResponseEntity.ok(updatedProof);
    }

    @DeleteMapping("/pendencies/{pendencyId}/proofs/{proofId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'PARTICIPANT')") 
    public ResponseEntity<Void> deletePaymentProof(
            @PathVariable String pendencyId,
            @PathVariable String proofId) {
        
        financeService.deletePaymentProof(pendencyId, proofId);
        return ResponseEntity.noContent().build();
    }

    
    // DESPESAS (EXPENSES)
    

    @PostMapping("/expenses")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense) { 
        Expense savedExpense = financeService.createExpense(expense);
        return ResponseEntity.status(201).body(savedExpense);
    }

    @GetMapping("/expenses")
    public ResponseEntity<Page<Expense>> listExpenses(
            @RequestParam(required = false) String cycle,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Expense> expenses = financeService.listExpenses(cycle, category, pageable);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/expenses/{expenseId}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable String expenseId) {
        return financeService.getExpenseById(expenseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/expenses/{expenseId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteExpense(@PathVariable String expenseId) {
        financeService.deleteExpense(expenseId);
        return ResponseEntity.noContent().build();
    }

    
    // RELATÓRIOS FINANCEIROS
    

    @GetMapping("/statement")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Object> getFinanceStatement( 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Object statement = financeService.getFinanceStatement(startDate, endDate);
        return ResponseEntity.ok(statement);
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Object> getFinanceBalance() { 
        Object balance = financeService.getFinanceBalance();
        return ResponseEntity.ok(balance);
    }
}