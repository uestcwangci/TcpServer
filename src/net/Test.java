package net;

import utils.ByteUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Test {

    private static long getUTCTimeStr() {
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();// 返回的就是UTC时间
    }


    public static void main(String[] args) {
//        long a = getUTCTimeStr();
//        System.out.println(a);
//        System.out.println((int) a % 100000000);
//        System.out.println(Long.valueOf(a).intValue());
//        System.out.println(Long.valueOf(a % 100000000).intValue());

        String a = "stop";
        byte[] bytes = a.getBytes();
        System.out.println(Arrays.toString(bytes));
        String s = new String(bytes, 0, 4);
        System.out.println(s);
    }
}
