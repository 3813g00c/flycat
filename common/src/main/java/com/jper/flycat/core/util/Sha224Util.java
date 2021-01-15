package com.jper.flycat.core.util;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author ywxiang
 * @date 2021/1/15 下午8:33
 */
public class Sha224Util {

    /**
     * 利用Apache的工具类实现SHA-256加密
     *
     * @param str 加密前的报文
     * @return 加密前的报文
     */
    public static String getSha224Str(String str) {
        MessageDigest messageDigest;
        String enCodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-224");
            byte[] hash = messageDigest.digest(str.getBytes(StandardCharsets.UTF_8));
            enCodeStr = Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return enCodeStr;
    }
}
