package ufms.facoffe.finance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ufms.facoffe.finance.domain.PaymentProof;

@Repository
public interface PaymentProofRepository extends JpaRepository<PaymentProof, String>{ 
    List<PaymentProof> findByPendingId(String pendingId);

}
