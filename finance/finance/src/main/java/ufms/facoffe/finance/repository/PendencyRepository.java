package ufms.facoffe.finance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.domain.enums.PendencyStatus;

@Repository
public interface PendencyRepository extends JpaRepository<FinancialPending, String> {
    
    @Query("SELECT f FROM FinancialPending f WHERE " +
           "(:userId IS NULL OR f.userId = :userId) AND " +
           "(:cycle IS NULL OR f.cycle = :cycle) AND " +
           "(:status IS NULL OR f.status = :status)")
    Page<FinancialPending> findWithFilters(
            @Param("userId") String userId, 
            @Param("cycle") String cycle, 
            @Param("status") PendencyStatus status, 
            Pageable pageable);
}