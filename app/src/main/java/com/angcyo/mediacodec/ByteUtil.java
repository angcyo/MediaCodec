package com.angcyo.mediacodec;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by robi on 2016-03-21 15:20.
 */
public class ByteUtil {
    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    public static byte[] intsToBytes(int[] values) {
        byte[] src = new byte[4 * values.length];
        for (int i = 0; i < values.length; i++) {
            byte[] bytes = intToBytes(values[i]);

            for (int j = 0; j < bytes.length; j++) {
                src[i * 4 + j] = bytes[j];
            }
//            System.arraycopy(bytes, 0, src, i * 4, bytes.length);
        }
        return src;
    }

    public static int[] bytesToInts(byte[] values) {
        int[] src = new int[values.length / 4];
        for (int i = 0; i < values.length / 4; i++) {
            byte[] bytes = new byte[4];
            for (int j = 0; j < 4; j++) {
                bytes[j] = values[i * 4 + j];
            }
            src[i] = bytesToInt(bytes, 0);
        }

        return src;
    }

    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /** Needed for the Digest Access Authentication. */
    private String computeMd5Hash(String buffer) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            return bytesToHex(md.digest(buffer.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException ignore) {
        } catch (UnsupportedEncodingException e) {}
        return "";
    }
}
