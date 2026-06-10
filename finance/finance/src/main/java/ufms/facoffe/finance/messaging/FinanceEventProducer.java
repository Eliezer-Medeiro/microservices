package ufms.facoffe.finance.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ufms.facoffe.finance.domain.FinancialPending;
import ufms.facoffe.finance.dto.events.FinancialPendencyCreatedEvent;
import ufms.facoffe.finance.dto.events.FinancialPendencyCreatedPayload;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class FinanceEventProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "finance.pendency-created";
    public void publishPendencyCreated(FinancialPending pendency) {
        FinancialPendencyCreatedPayload payload = new FinancialPendencyCreatedPayload(
                String.valueOf(pendency.getId()),
                pendency.getSource(),
                pendency.getSourceId(),
                pendency.getUserId(),
                pendency.getCycle(),
                pendency.getAmount(),
                pendency.getStatus().name()
        );

        FinancialPendencyCreatedEvent event = new FinancialPendencyCreatedEvent(
                UUID.randomUUID().toString(),
                "FinancialPendencyCreated",
                LocalDateTime.now(),
                payload
        );

        // Envia utilizando o ID do utilizador como chave para garantir a ordem dos eventos por utilizador
        kafkaTemplate.send(TOPIC, pendency.getUserId(), event);
    }
}