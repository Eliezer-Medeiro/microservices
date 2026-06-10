package ufms.facoffe.finance.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ufms.facoffe.finance.dto.events.UserDeactivatedEvent;
import ufms.facoffe.finance.service.FinanceService;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    @Autowired
    private FinanceService financeService;

    @KafkaListener(
        topics = "users.deactivated", 
        groupId = "finance-service-group",
        containerFactory = "kafkaListenerContainerFactory" // Configurado para ACK manual se necessário
    )
    public void consumeUserDeactivated(UserDeactivatedEvent event) {
        log.info("Evento recebido - Utilizador Desativado: {}", event.payload().userId());
        
        try {
            // Chamada ao serviço para aplicar a regra de negócio
            financeService.handleUserDeactivation(event.payload().userId(), event.eventId());
        } catch (Exception e) {
            log.error("Erro ao processar evento de desativação do utilizador: {}", event.payload().userId(), e);
            // Lançar a exceção ativa as políticas de Retry/DLT configuradas no Spring Kafka
            throw e; 
        }
    }
}