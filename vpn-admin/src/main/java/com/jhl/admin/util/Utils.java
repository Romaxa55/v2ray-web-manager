package com.jhl.admin.util;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.jhl.admin.constant.KVConstant;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public final class Utils {
    private final static Interner<Object> STRING_WEAK_POLL = Interners.newWeakInterner();



    public static String generateVCode()  {

            SecureRandom random = new SecureRandom();
            return random.nextInt(100000) + "";



    }

    public static    Interner<Object> getInternersPoll(){
        return STRING_WEAK_POLL;
        }

    public static String getCharAndNum(int length) {

        SecureRandom random = new SecureRandom();

        StringBuffer valSb = new StringBuffer();

        String charStr = "0123456789abcdefghijklmnopqrstuvwxyz";

        int charLength = charStr.length();


        for (int i = 0; i < length; i++) {

            int index = random.nextInt(charLength);

            valSb.append(charStr.charAt(index));

        }

        return valSb.toString();

    }


    public static Date getDateBy(int day) {

        return getDateBy(new Date(), day, Calendar.DAY_OF_YEAR);
    }

    public static Date getDateBy(Date date, int addTime, int type) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(type, addTime);
        return calendar.getTime();
    }


    public static Date formatDate(Date date, SimpleDateFormat simpleDateFormat) {
        SimpleDateFormat thisSDF = new SimpleDateFormat(KVConstant.YYYYMMddHH);

        if (simpleDateFormat != null) thisSDF = simpleDateFormat;
        try {
            return thisSDF.parse(thisSDF.format(date));
        } catch (ParseException e) {
            throw new RuntimeException("ошибка преобразования", e);
        }

    }

    public static void main(String[] args) {
        System.out.println(formatDate(new Date(), null));
    }


}
