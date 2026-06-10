package ufms.facoffe.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ufms.facoffe.finance.domain.Expense;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.domain.PaymentProof;
import ufms.facoffe.finance.domain.enums.PaymentProofStatus;
import ufms.facoffe.finance.domain.enums.PendencyStatus;
import ufms.facoffe.finance.service.FinanceService;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

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

    @GetMapping("/pendencies/{id}")
    public ResponseEntity<FinancialPending> getPendencyById(@PathVariable Long id) {
        return financeService.getPendencyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/pendencies/{pendencyId}/payment-proofs")
    public ResponseEntity<PaymentProof> submitPaymentProof(
            @PathVariable Long pendencyId, 
            @RequestBody PaymentProof proof) {
        
        PaymentProof savedProof = financeService.submitPaymentProof(pendencyId, proof);
        return ResponseEntity.status(201).body(savedProof);
    }

    @PatchMapping("/payment-proofs/{proofId}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PaymentProof> reviewPaymentProof(
            @PathVariable Long proofId,
            @RequestParam PaymentProofStatus newStatus,
            @RequestParam(required = false) String rejectionReason,
            @RequestParam String managerId) {

        PaymentProof updatedProof = financeService.reviewPaymentProof(proofId, newStatus, rejectionReason, managerId);
        return ResponseEntity.ok(updatedProof);
    }

    @PostMapping("/expenses")
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense) {
        Expense savedExpense = financeService.createExpense(expense);
        return ResponseEntity.status(201).body(savedExpense);
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<Expense>> listExpenses() {
        return ResponseEntity.ok(financeService.listExpenses());
    }
}
