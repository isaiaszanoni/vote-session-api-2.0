package com.siccase.vote_session_api.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CpfUtils {

    public static String getNumericDigitsOfCpf(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }

    public static String applyCpfMask(String cpf) {
        String numericDigitsCpf = getNumericDigitsOfCpf(cpf);

        return numericDigitsCpf.substring(0, 3) + "." +
                numericDigitsCpf.substring(3, 6) + "." +
                numericDigitsCpf.substring(6, 9) + "-" +
                numericDigitsCpf.substring(9, 11);
    }

    public static List<Integer> convertsToListOfNumber(String value) {
        return value.chars()
                .map(Character::getNumericValue)
                .boxed()
                .toList();
    }
}
