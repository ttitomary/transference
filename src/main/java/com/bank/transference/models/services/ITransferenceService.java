package com.bank.transference.models.services;

import com.bank.transference.models.documents.Transference;
import com.bank.transference.models.enums.TransferenceStatus;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ITransferenceService {
    Mono<List<Transference>> findAll();
    Mono<Transference> find(String id);
    Mono<Transference> create(Transference transference);
    Mono<Transference> update(String id, Transference transference);
    Mono<Object> delete(String id);
    Mono<Transference> updateStatus(String id, TransferenceStatus status);
}
