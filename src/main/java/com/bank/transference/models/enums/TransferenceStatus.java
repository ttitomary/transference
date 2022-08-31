package com.bank.transference.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransferenceStatus {
    CREATED(0),
    IN_PROCESS(1),
    IN_VALIDATION(2),
    IN_PAYMENT_PROCESS(3),
    ACCEPTED(4),
    REFUSED(5);
    public final int value;
}
