package com.bank.transference.models.documents;

import com.bank.transference.models.enums.TransferenceStatus;
import com.bank.transference.models.enums.TransferenceType;
import com.bank.transference.models.utils.Audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transfers")
public class Transference extends Audit {
    @Id
    private String id;
    @NotNull(message = "idSender must not be null")
    private Client idSender;
    @NotNull(message = "idReceiver must not be null")
    private Client idReceiver;
    @NotNull(message = "bootcoins must not be null")
    private Float bootcoins;
    @NotNull(message = "transferenceType must not be null")
    private TransferenceType transferenceType;
    private Float amount;
    private String transferNumber;
    private TransferenceStatus status;
}
