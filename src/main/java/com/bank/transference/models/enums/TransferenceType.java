package com.bank.transference.models.enums;

import lombok.*;

@Getter
@AllArgsConstructor
public enum TransferenceType {
    BUY(0),
    SELL(1);
    public final int value;
}
