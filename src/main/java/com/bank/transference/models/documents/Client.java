package com.bank.transference.models.documents;

import com.bank.transference.models.utils.Audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Client extends Audit {
    private String idClient;
    private String phone;
}
