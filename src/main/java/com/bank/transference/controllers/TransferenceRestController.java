package com.bank.transference.controllers;

import com.bank.transference.handler.ResponseHandler;
import com.bank.transference.models.documents.Transference;
import com.bank.transference.models.enums.TransferenceStatus;
import com.bank.transference.models.kafka.RequestWallet;
import com.bank.transference.models.kafka.RequestYanki;
import com.bank.transference.models.kafka.ResponseTransference;
import com.bank.transference.models.services.ITransferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transference")
public class TransferenceRestController {
    @Autowired
    private ITransferenceService transferenceService;

    @Autowired
    private KafkaTemplate<String, RequestWallet> walletKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, RequestYanki> yankiKafkaTemplate;

    private static final Logger log = LoggerFactory.getLogger(TransferenceRestController.class);

    @PostMapping
    public Mono<ResponseEntity<Object>> create(@Validated @RequestBody Transference trans) {
        log.info("[INI] create");
        return transferenceService.create(trans)
                .doOnNext(t -> {
                    log.info(t.toString());

                    var requestWallet = RequestWallet.builder()
                            .idSender(trans.getIdSender().getIdClient())
                            .idReceiver(trans.getIdReceiver().getIdClient())
                            .bootcoins(trans.getBootcoins())
                            .idTransference(t.getId())
                            .transferenceType(trans.getTransferenceType())
                            .build();
                    walletKafkaTemplate.send("wallet-check", requestWallet);

                    log.info(requestWallet.toString());
                })
                .flatMap(o -> Mono.just(ResponseHandler.response("Done", HttpStatus.OK, o)))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .doFinally(fin -> log.info("[END] create"));
    }

    @GetMapping
    public Mono<ResponseEntity<Object>> findAll() {
        log.info("[INI] findAll");
        return transferenceService.findAll()
                .doOnNext(t -> log.info(t.toString()))
                .flatMap(o -> Mono.just(ResponseHandler.response("Done", HttpStatus.OK, o)))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .doFinally(fin -> log.info("[END] findAll"));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Object>> find(@PathVariable String id) {
        log.info("[INI] find");
        return transferenceService.find(id)
                .doOnNext(t -> log.info(t.toString()))
                .map(o -> ResponseHandler.response("Done", HttpStatus.OK, o))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .doFinally(fin -> log.info("[END] find"));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> update(@PathVariable("id") String id,@Validated @RequestBody Transference trans) {
        log.info("[INI] update");
        return transferenceService.update(id,trans)
                .doOnNext(t -> {
                    log.info(t.toString());

                    var requestWallet = RequestWallet.builder()
                            .idSender(t.getIdSender().getIdClient())
                            .idReceiver(trans.getIdReceiver().getIdClient())
                            .bootcoins(trans.getBootcoins())
                            .idTransference(t.getId())
                            .transferenceType(t.getTransferenceType())
                            .build();
                    walletKafkaTemplate.send("wallet-check", requestWallet);

                    log.info(requestWallet.toString());
                })
                .flatMap(o -> Mono.just(ResponseHandler.response("Done", HttpStatus.OK, o)))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(ResponseHandler.response("Transfer cannot be updated", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] update"));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable("id") String id) {
        log.info("[INI] delete");
        return transferenceService.delete(id)
                .flatMap(o -> Mono.just(ResponseHandler.response("Done", HttpStatus.OK, null)))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(ResponseHandler.response("Error", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] delete"));
    }

    @KafkaListener(topics = "transference_wallet-check", groupId = "wallet")
    public void sendCheckBootcoins(@Payload ResponseTransference responseTransference)
    {
        log.info("[INI] sendCheckBootcoins");
        var boolStatus = responseTransference.getStatus();
        log.info(String.format("boolStatus -> %s", boolStatus.toString()));

        var id = responseTransference.getIdTransference();
        log.info(String.format("id -> %s", id.toString()));

        if(Boolean.TRUE.equals(boolStatus)){
            transferenceService.updateStatus(id, TransferenceStatus.IN_PROCESS);

            transferenceService.find(id).subscribe(transfer -> {
                log.info(transfer.toString());
                var requestYanki = RequestYanki.builder()
                        .phoneNumberSender(transfer.getIdSender().getPhone())
                        .phoneNumberReceiver(transfer.getIdReceiver().getPhone())
                        .mont(transfer.getAmount())
                        .idTransference(transfer.getId())
                        .transferenceType(transfer.getTransferenceType())
                        .build();
                yankiKafkaTemplate.send("yanki-check", requestYanki);

                log.info(requestYanki.toString());
            });
        }
        else{
            transferenceService.updateStatus(id, TransferenceStatus.REFUSED);
        }
        log.info("[END] sendCheckBootcoins");
    }

    @KafkaListener(topics = "transference_yanki-check", groupId = "yanki")
    public void sendCheckMont(@Payload ResponseTransference responseTransference)
    {
        log.info("[INI] sendCheckMont");
        var boolStatus = responseTransference.getStatus();
        log.info(String.format("boolStatus -> %s", boolStatus.toString()));

        var id = responseTransference.getIdTransference();
        log.info(String.format("id -> %s", id.toString()));

        if(Boolean.TRUE.equals(boolStatus)){
            transferenceService.updateStatus(id, TransferenceStatus.IN_VALIDATION);

            transferenceService.find(id).subscribe(transfer -> {
                log.info(transfer.toString());
                var requestWallet = RequestWallet.builder()
                        .idSender(transfer.getIdSender().getIdClient())
                        .idReceiver(transfer.getIdReceiver().getIdClient())
                        .bootcoins(transfer.getBootcoins())
                        .idTransference(transfer.getId())
                        .transferenceType(transfer.getTransferenceType())
                        .build();
                walletKafkaTemplate.send("wallet-update", requestWallet);

                log.info(requestWallet.toString());
            });
        }
        else{
            transferenceService.updateStatus(id, TransferenceStatus.REFUSED);
        }
        log.info("[END] sendCheckMont");
    }

    @KafkaListener(topics = "transference_wallet-update", groupId = "wallet")
    public void sendUpdateBootcoins(@Payload ResponseTransference responseTransference)
    {
        log.info("[INI] sendUpdateBootcoins");
        var boolStatus = responseTransference.getStatus();
        log.info(String.format("boolStatus -> %s", boolStatus.toString()));

        var id = responseTransference.getIdTransference();
        log.info(String.format("id -> %s", id.toString()));

        if(Boolean.TRUE.equals(boolStatus)){
            transferenceService.updateStatus(id, TransferenceStatus.IN_PAYMENT_PROCESS);

            transferenceService.find(id).subscribe(transfer -> {
                log.info(transfer.toString());
                var requestYanki = RequestYanki.builder()
                        .phoneNumberSender(transfer.getIdSender().getPhone())
                        .phoneNumberReceiver(transfer.getIdReceiver().getPhone())
                        .mont(transfer.getAmount())
                        .idTransference(transfer.getId())
                        .transferenceType(transfer.getTransferenceType())
                        .build();
                yankiKafkaTemplate.send("yanki-update", requestYanki);

                log.info(requestYanki.toString());
            });
        }
        else{
            transferenceService.updateStatus(id, TransferenceStatus.REFUSED);
        }
        log.info("[END] sendUpdateBootcoins");
    }

    @KafkaListener(topics = "transference_yanki-update", groupId = "yanki")
    public void sendUpdateMonts(@Payload ResponseTransference responseTransference)
    {
        log.info("[INI] sendUpdateMonts");
        var boolStatus = responseTransference.getStatus();
        log.info(String.format("boolStatus -> %s", boolStatus.toString()));

        var id = responseTransference.getIdTransference();
        log.info(String.format("id -> %s", id.toString()));

        if(Boolean.TRUE.equals(boolStatus)){
            transferenceService.updateStatus(id, TransferenceStatus.ACCEPTED);
        }
        else{
            transferenceService.updateStatus(id, TransferenceStatus.REFUSED);
        }
        log.info("[END] sendUpdateMonts");
    }
}