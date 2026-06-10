package ufms.facoffe.finance.dto.events;

import java.time.LocalDateTime;

public record FinancialPendencyCreatedEvent(
    String eventId,
    String eventType,
    LocalDateTime timestamp,
    FinancialPendencyCreatedPayload payload
) {}
