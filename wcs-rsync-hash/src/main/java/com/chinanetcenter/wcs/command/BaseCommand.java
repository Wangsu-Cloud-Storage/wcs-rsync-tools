package com.chinanetcenter.wcs.command;

import com.chinanetcenter.api.util.EncodeUtils;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class BaseCommand {
    private static Logger logger = Logger.getLogger(BaseCommand.class);
    public static final long KB = 1024;
    public static final long MB = 1024 * KB;
    public static final long GB = 1024 * MB;

    public static String getSignature(byte[] data, String key) {
        byte[] keyBytes = key.getBytes();
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
        Mac mac;
        StringBuilder sb = new StringBuilder();
        try {
            mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data);

            for (byte b : rawHmac) {
                sb.append(byteToHexString(b));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static String byteToHexString(byte ib) {
        char[] Digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] ob = new char[2];
        ob[0] = Digit[(ib >>> 4) & 0X0f];
        ob[1] = Digit[ib & 0X0F];
        return new String(ob);
    }

    public static String produceToken(String sk, String encodeKey) {
        String skValue = "";
        try {
            String singSk = getSignature(encodeKey.getBytes("utf-8"), sk);
            skValue = EncodeUtils.urlsafeEncode(singSk);
        } catch (Exception e) {
            logger.error("生成token出错:", e);
        }
        return skValue;
    }

}
