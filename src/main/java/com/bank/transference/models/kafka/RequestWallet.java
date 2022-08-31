package com.bank.transference.models.kafka;

import com.bank.transference.models.enums.TransferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RequestWallet {
    private String idSender;
    private String idReceiver;
    private Float bootcoins;
    private String idTransference;
    private TransferenceType transferenceType;
}
