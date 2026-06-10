package ufms.facoffe.finance.dto.events;

import java.time.LocalDateTime;

public record UserDeactivatedEvent(
    String eventId,
    String eventType,
    LocalDateTime timestamp,
    UserDeactivatedPayload payload
) {}
