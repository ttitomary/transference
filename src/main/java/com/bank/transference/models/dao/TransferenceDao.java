package com.bank.transference.models.dao;

import com.bank.transference.models.documents.Transference;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface TransferenceDao extends ReactiveMongoRepository<Transference, String> {
}
