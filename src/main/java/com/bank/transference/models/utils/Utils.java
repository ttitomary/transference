package com.bank.transference.models.utils;

import com.bank.transference.models.enums.TransferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    public static String createTransferNumber() {
        log.info("[INI] createTransferNumber");
        Random r = new Random();
        String chain = "OP-0";

        int number = r.nextInt(900000) + 100000;
        String numbers = String.valueOf(number);
        log.info(String.format("numbers -> %s", numbers));

        String concat = chain + numbers;
        log.info(String.format("concat -> %s", concat));

        log.info("[END] createTransferNumber");
        return concat;
    }

    public static Float calculateAmount(Float bootcoins, TransferenceType type){
        log.info("[INI] createTransferNumber");

        Float amount;
        if(type.equals(TransferenceType.BUY))
            amount = bootcoins * (5);
        else
            amount = bootcoins * (5.5f);

        log.info(String.format("amount -> %s", amount.toString()));
        log.info("[END] createTransferNumber");
        return amount;
    }
}
