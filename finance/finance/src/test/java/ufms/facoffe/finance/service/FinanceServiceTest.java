package ufms.facoffe.finance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ufms.facoffe.finance.domain.Expense;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.domain.PaymentProof;
import ufms.facoffe.finance.domain.enums.PaymentProofStatus;
import ufms.facoffe.finance.domain.enums.PendencyStatus;
import ufms.facoffe.finance.messaging.FinanceEventProducer;
import ufms.facoffe.finance.repository.ExpenseRepository;
import ufms.facoffe.finance.repository.PaymentProofRepository;
import ufms.facoffe.finance.repository.PendencyRepository;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private PendencyRepository pendencyRepository;

    @Mock
    private PaymentProofRepository paymentProofRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private FinanceEventProducer financeEventProducer;

    @InjectMocks
    private FinanceService financeService;

    private FinancialPending pendency;
    private PaymentProof proof;

    @BeforeEach
    void setUp() {
        // Configuração inicial de uma Pendência
        pendency = new FinancialPending();
        pendency.setId("1");
        pendency.setUserId("user_123");
        pendency.setAmount(new BigDecimal("50.00"));
        pendency.setStatus(PendencyStatus.PENDING);

        // Configuração inicial de um Comprovativo
        proof = new PaymentProof();
        proof.setId("100");
        proof.setPendingId("1");
        proof.setAmount(new BigDecimal("50.00"));
        proof.setStatus(PaymentProofStatus.WAITING_APPROVAL);
    }

    @Test
    void deveSubmeterComprovativoEAlterarStatusDaPendencia() {
        // Arrange
        when(pendencyRepository.findById("1")).thenReturn(Optional.of(pendency));
        when(paymentProofRepository.save(any(PaymentProof.class))).thenReturn(proof);
        when(pendencyRepository.save(any(FinancialPending.class))).thenReturn(pendency);

        PaymentProof newProof = new PaymentProof();
        
        // Act
        PaymentProof result = financeService.submitPaymentProof("1", newProof);

        // Assert
        assertEquals(PendencyStatus.WAITING_VALIDATION, pendency.getStatus()); // Atualizado para refletir o serviço
        assertEquals(PaymentProofStatus.WAITING_APPROVAL, result.getStatus());
        verify(pendencyRepository).save(pendency);
        verify(paymentProofRepository).save(newProof);
    }

    @Test
    void deveValidarComprovativoEAlterarStatusParaPago() {
        // Arrange
        when(paymentProofRepository.findById("100")).thenReturn(Optional.of(proof));
        when(pendencyRepository.findById("1")).thenReturn(Optional.of(pendency));
        when(paymentProofRepository.save(any(PaymentProof.class))).thenReturn(proof);
        when(pendencyRepository.save(any(FinancialPending.class))).thenReturn(pendency);

        // Act - Alterado para decidePaymentProof
        PaymentProof result = financeService.decidePaymentProof("1", "100", PaymentProofStatus.VALIDATED, null, "manager_999");

        // Assert
        assertEquals(PaymentProofStatus.VALIDATED, result.getStatus());
        assertEquals("manager_999", result.getValidatedBy());
        assertEquals(PendencyStatus.PAID, pendency.getStatus());
        assertNotNull(pendency.getPaidAt());
        
        verify(pendencyRepository).save(pendency);
        verify(paymentProofRepository).save(proof);
    }

    @Test
    void deveRejeitarComprovativoEMarcarPendenciaComoRejeitada() {
        // Arrange
        when(paymentProofRepository.findById("100")).thenReturn(Optional.of(proof));
        when(pendencyRepository.findById("1")).thenReturn(Optional.of(pendency));
        when(paymentProofRepository.save(any(PaymentProof.class))).thenReturn(proof);
        when(pendencyRepository.save(any(FinancialPending.class))).thenReturn(pendency);

        // Act - Alterado para decidePaymentProof
        PaymentProof result = financeService.decidePaymentProof("1", "100", PaymentProofStatus.REJECTED, "Imagem ilegível", "manager_999");

        // Assert
        assertEquals(PaymentProofStatus.REJECTED, result.getStatus());
        assertEquals("manager_999", result.getRejectedBy());
        assertEquals("Imagem ilegível", result.getRejectionReason());
        assertEquals(PendencyStatus.REJECTED, pendency.getStatus());
        
        verify(pendencyRepository).save(pendency);
        verify(paymentProofRepository).save(proof);
    }

    @Test
    void deveLancarExcecaoAoRevisarComprovativoJaProcessado() {
        // Arrange
        proof.setStatus(PaymentProofStatus.VALIDATED); // Já processado
        when(paymentProofRepository.findById("100")).thenReturn(Optional.of(proof));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            // Alterado para decidePaymentProof
            financeService.decidePaymentProof("1", "100", PaymentProofStatus.VALIDATED, null, "manager_999");
        });

        assertTrue(exception.getMessage().contains("estado atual")); // Mensagem ajustada para a exceção atual do Service
        verify(pendencyRepository, never()).save(any());
    }

    @Test
    void deveCriarPendenciaEPublicarEvento() {
        // Arrange
        when(pendencyRepository.save(any(FinancialPending.class))).thenReturn(pendency);

        // Act
        FinancialPending result = financeService.createPendency(pendency);

        // Assert
        assertEquals(PendencyStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());
        
        // Verifica se o produtor Kafka foi chamado para publicar o evento
        verify(financeEventProducer, times(1)).publishPendencyCreated(pendency);
    }

    @Test
    void deveCancelarPendenciasDeUtilizadorDesativado() {
        // Arrange
        String userId = "user_123";
        FinancialPending pendency2 = new FinancialPending();
        pendency2.setId("2");
        pendency2.setUserId(userId);
        pendency2.setStatus(PendencyStatus.PENDING); 

        when(pendencyRepository.findAll()).thenReturn(List.of(pendency, pendency2));

        // Act
        financeService.handleUserDeactivation(userId, "evento_xyz");

        // Assert
        assertEquals(PendencyStatus.CANCELLED, pendency.getStatus()); 
        assertEquals(PendencyStatus.CANCELLED, pendency2.getStatus()); 
        verify(pendencyRepository, times(2)).save(any(FinancialPending.class));
    }

    @Test
    void deveLancarExcecaoAoCriarDespesaComValorInvalido() {
        // Arrange
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.ZERO);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            financeService.createExpense(expense);
        });

        assertTrue(exception.getMessage().contains("maior que zero"));
        verify(expenseRepository, never()).save(any());
    }
}