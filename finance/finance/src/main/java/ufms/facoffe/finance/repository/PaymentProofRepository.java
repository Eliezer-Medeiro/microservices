package ufms.facoffe.finance.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ufms.facoffe.finance.domain.PaymentProof;
import ufms.facoffe.finance.domain.enums.PaymentProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ufms.facoffe.finance.domain.PaymentProof;

@Repository
public interface PaymentProofRepository extends JpaRepository<PaymentProof, String> {
    Page<PaymentProof> findByPendingId(String pendingId, Pageable pageable);
    Page<PaymentProof> findByPendingIdAndStatus(String pendingId, PaymentProofStatus status, Pageable pageable);
}
