package com.bank.transference.models.services.impl;

import com.bank.transference.models.dao.TransferenceDao;
import com.bank.transference.models.documents.Transference;
import com.bank.transference.models.enums.TransferenceStatus;
import com.bank.transference.models.enums.TransferenceType;
import com.bank.transference.models.services.ITransferenceService;
import com.bank.transference.models.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransferenceService implements ITransferenceService {
    @Autowired
    private TransferenceDao dao;

    @Autowired
    private ReactiveRedisTemplate<String, Transference> redisTemplate;

    @Override
    public Mono<List<Transference>> findAll() {
        return dao.findAll().collectList();
    }

    @Override
    public Mono<Transference> find(String id) {
        return redisTemplate.opsForValue().get(id)
                .switchIfEmpty(dao.findById(id)
                        .doOnNext(trans -> redisTemplate.opsForValue()
                                .set(trans.getId(), trans)
                                .subscribe(aBoolean -> {
                                    redisTemplate.expire(id, Duration.ofMinutes(10)).subscribe();
                                })));
    }

    @Override
    public Mono<Transference> create(Transference transference) {
        transference.setCreatedDate(LocalDateTime.now());
        transference.setAmount(Utils.calculateAmount(transference.getBootcoins(), transference.getTransferenceType()));
        transference.setTransferNumber(Utils.createTransferNumber());
        transference.setStatus(TransferenceStatus.CREATED);
        return dao.save(transference)
                .doOnNext(trans -> redisTemplate.opsForValue()
                        .set(trans.getId(), trans)
                        .subscribe(aBoolean -> {
                            redisTemplate.expire(trans.getId(), Duration.ofMinutes(10)).subscribe();
                        }));
    }

    @Override
    public Mono<Transference> update(String id, Transference transference) {
        return dao.findById(id).flatMap(transfer -> {
            if(transfer.getStatus().equals(TransferenceStatus.CREATED) || transfer.getStatus().equals(TransferenceStatus.REFUSED)){
                transfer.setUpdateDate(LocalDateTime.now());
                transfer.setIdReceiver(transference.getIdReceiver());
                transfer.setBootcoins(transference.getBootcoins());
                transfer.setAmount(Utils.calculateAmount(transference.getBootcoins(), transfer.getTransferenceType()));
                transfer.setStatus(TransferenceStatus.CREATED);
                redisTemplate.opsForValue().delete(id).subscribe();
                return dao.save(transfer)
                        .doOnNext(trans -> redisTemplate.opsForValue()
                                .set(trans.getId(), trans)
                                .subscribe(aBoolean -> {
                                    redisTemplate.expire(id, Duration.ofMinutes(10));
                                }));
            }
            else{
                return Mono.empty();
            }
        }).switchIfEmpty(Mono.empty());
    }

    @Override
    public Mono<Object> delete(String id) {
        return dao.existsById(id).flatMap(check -> {
            if (Boolean.TRUE.equals(check))
            {
                redisTemplate.opsForValue().delete(id).subscribe();
                return dao.deleteById(id).then(Mono.just(true));
            }
            else
                return Mono.empty();
        });
    }

    @Override
    public Mono<Transference> updateStatus(String id, TransferenceStatus status) {
        return dao.findById(id).flatMap(transfer -> {
            transfer.setUpdateDate(LocalDateTime.now());
            transfer.setStatus(status);
            redisTemplate.opsForValue().delete(id).subscribe();
            return dao.save(transfer)
                    .doOnNext(trans -> redisTemplate.opsForValue()
                            .set(trans.getId(), trans)
                            .subscribe(aBoolean -> {
                                redisTemplate.expire(id, Duration.ofMinutes(10));
                            }));
        }).switchIfEmpty(Mono.empty());
    }
}
