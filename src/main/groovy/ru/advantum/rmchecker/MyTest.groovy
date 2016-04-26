package ru.advantum.rmchecker

import java.security.MessageDigest
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Created by Kotin on 22.03.2016.
 */
class MyTest {
    static void main(String... args) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        formatter.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))

        Date lastChanged = formatter.parse("2016-03-22T13:50:44Z")
        ZoneOffset zone = ZoneOffset.UTC
        def ldt =     lastChanged





        println MessageDigest.getInstance("MD5").digest("login:password".bytes)//.encodeHex().toString()
//        println lastChanged.toInstant()
//        LocalDateTime ldt = LocalDateTime.ofInstant(lastChanged.toInstant().atOffset(ZoneOffset.UTC), ZoneId.systemDefault());
//        println ldt
    }
}
