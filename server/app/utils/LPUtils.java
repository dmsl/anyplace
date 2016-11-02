/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

package utils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.UUID;

/**
 * Created by lambros on 2/4/14.
 */
public class LPUtils {

    // Used by the secure encryption/decryption algorithms
    private final static int SECURE_ITERATIONS = 1000;
    private final static int SECURE_KEY_LENGTH = 128;

    // used by the secure psedo random number generator
    private final static int PRNG_SEED = 16; // 256 best but very slow

    public static String getRandomUUID(){
        return UUID.randomUUID().toString();
    }

    /**
     * Return a new random string
     */
    public static String generateRandomToken(){
        SecureRandom secureRandom;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(secureRandom.generateSeed(PRNG_SEED));
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] dig = digest.digest((secureRandom.nextLong() + "").getBytes());
            return binaryToHex(dig);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String hashStringHex(String input){
        return binaryToHex(hashString(input));
    }

    public static byte[] hashString(String input){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            md.update( (input + salt).getBytes("UTF-8") );
            byte byteData[] = md.digest();
            return byteData;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Derives the key that will be used by the encryption and decryption
     * algorithms below.
     *
     * site: http://nelenkov.blogspot.ch/2012/04/using-password-based-encryption-on.html
     *
     * @param salt The salt that is being used
     * @param password The password of the encryption
     * @return The SecretKey for the encryption/decryption
     */
    public static SecretKey deriveKeyPbkdf2(byte[] salt, String password){
        int iterationCount = SECURE_ITERATIONS;
        int keyLength = SECURE_KEY_LENGTH;
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt,iterationCount, keyLength);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static String binaryToHex(byte[] ba){
        if( ba == null || ba.length == 0 ){
            return null;
        }
        StringBuilder sb = new StringBuilder(ba.length * 2);
        String hexNumber;
        for( int x=0, sz=ba.length; x<sz; x++ ){
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);
            sb.append(hexNumber.substring(hexNumber.length()-1));
        }
        return sb.toString();
    }

    public static byte[] hexToBinary(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }

        byte[] ba = new byte[hex.length() / 2];
        for (int i = 0; i < ba.length; i++) {
            ba[i] = (byte) Integer
                    .parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return ba;
    }
}
