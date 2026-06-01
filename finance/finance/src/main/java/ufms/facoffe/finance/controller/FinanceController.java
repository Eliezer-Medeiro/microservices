package ufms.facoffe.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Generated;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.domain.enums.PendencyStatus;
import ufms.facoffe.finance.repository.PendencyRepository;


@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Autowired
    private PendencyRepository pendencyRepository;

    @GetMapping("/pendencies")
    public ResponseEntity<?> listPendencies(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String cycle,
            @RequestParam(required = false) PendencyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);

        Page<FinancialPending> pendencies = pendencyRepository.findWithFilters(userId, cycle, status, pageable);
        return ResponseEntity.ok().body(pendencies);
    }
}
