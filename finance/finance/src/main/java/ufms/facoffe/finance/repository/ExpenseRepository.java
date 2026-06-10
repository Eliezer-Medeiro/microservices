package ufms.facoffe.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufms.facoffe.finance.domain.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
}
