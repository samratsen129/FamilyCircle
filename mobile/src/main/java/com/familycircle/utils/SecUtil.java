package com.familycircle.utils;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.util.Base64;

public class SecUtil {

    private static final String ALGORITHM_INSTANCE_TYPE = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_SPEC = "AES";

    public SecUtil() {

    }

    public static byte[] decryptAES256(final byte[] key, final byte[] iv, String base64EncodedCipherText) throws Exception {

        SecretKeySpec spec = new SecretKeySpec(key, SECRET_KEY_SPEC);
        byte[] plainBytes;

        Cipher cipher = Cipher.getInstance(ALGORITHM_INSTANCE_TYPE);
        cipher.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(iv));
        byte[] cipherBytes = Base64.decode(base64EncodedCipherText, Base64.DEFAULT);
        plainBytes = cipher.doFinal(cipherBytes);

        return plainBytes;
    }

    public static byte[] encryptToBytesAES256(final byte[] key, final byte[] iv, String plainText) throws Exception{

        SecretKeySpec spec = new SecretKeySpec(key, SECRET_KEY_SPEC);
        byte[] cipherBytes;

        Cipher cipher = Cipher.getInstance(ALGORITHM_INSTANCE_TYPE);
        cipher.init(Cipher.ENCRYPT_MODE, spec, new IvParameterSpec(iv));
        byte[] plainTextBytes = plainText.getBytes();
        cipherBytes = cipher.doFinal(plainTextBytes);

        return cipherBytes;
    }

    public static byte[] getKey(UUID uuid1, UUID uuid2, int keySize){

        if (keySize!=16 && keySize!=32) return null;

        if (keySize==32 && uuid2==null) return null;

        byte[] keyBytes = new byte[keySize];

        ByteBuffer key1Buffer = ByteBuffer.wrap(new byte[16]);
        key1Buffer.putLong(uuid1.getMostSignificantBits());
        key1Buffer.putLong(uuid1.getLeastSignificantBits());

        byte[] key1Bytes = key1Buffer.array();

        for(int index=0; index<16; index++){
            keyBytes[index] = key1Bytes[index];
        }

        if (keySize==32){
            ByteBuffer key2Buffer = ByteBuffer.wrap(new byte[16]);
            key2Buffer.putLong(uuid2.getMostSignificantBits());
            key2Buffer.putLong(uuid2.getLeastSignificantBits());

            byte[] key2Bytes = key2Buffer.array();

            for(int index=0, offset=16; index<16; index++, offset++){
                keyBytes[offset] = key2Bytes[index];
            }
        }

        return keyBytes;
    }

    public static byte[] generateSHA1Hash(String src) throws Exception {

        byte[] hashedBytes;
        final byte[] key = "abbjdf213mfdsafr3m".getBytes();
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secret = new SecretKeySpec(key, "HmacSHA1");
        mac.init(secret);

        byte[] srcBytes;
        srcBytes = src.getBytes();
        hashedBytes = mac.doFinal(srcBytes);


        return hashedBytes;
    }

}

