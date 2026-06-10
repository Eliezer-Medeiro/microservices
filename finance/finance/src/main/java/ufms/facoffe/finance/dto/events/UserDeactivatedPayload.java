package ufms.facoffe.finance.dto.events;

public record UserDeactivatedPayload(
    String userId,
    String reason
) {}
