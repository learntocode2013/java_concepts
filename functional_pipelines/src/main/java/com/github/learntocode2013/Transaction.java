package com.github.learntocode2013;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
    String cardNumber,
    BigDecimal amount,
    String merchantLocation,
    LocalDateTime dateTime
) {}
