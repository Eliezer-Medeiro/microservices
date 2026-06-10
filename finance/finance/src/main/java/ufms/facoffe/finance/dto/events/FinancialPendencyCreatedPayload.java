package ufms.facoffe.finance.dto.events;

import java.math.BigDecimal;

public record FinancialPendencyCreatedPayload(
    String pendencyId,
    String source,
    String sourceId,
    String userId,
    String cycle,
    BigDecimal amount,
    String status
) {}
