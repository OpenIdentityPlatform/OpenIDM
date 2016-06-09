// ========================================================================
// Copyright (c) 1998-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses.
// ========================================================================

package org.forgerock.openidm.crypto.impl;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.Console;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * This utility helps obfuscate passwords to prevent casual observation. It is not securely encrypted.
 *
 * To allow for sharing the same mechanism for jetty config and the rest of the system this
 * is based on the jetty Password class.
 */
public class Main {

    public static final String __OBFUSCATE = "OBF:";
    public static final String __CRYPT = "CRYPT:";
    public static final String __CONSOLE = "CONSOLE";

    private static final String CRYPT_ALGORITHM = "AES";

    private static final byte[] NON_SECRET_KEY = {-50, 50, -16, -26, -99, -61, 94, 45, 26, 75, -39, -44, -48, -75, 40, 4};

    private static final String[] HEX_TABLE = new String[]{
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0a", "0b", "0c", "0d", "0e", "0f",
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b", "1c", "1d", "1e", "1f",
            "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "2a", "2b", "2c", "2d", "2e", "2f",
            "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "3a", "3b", "3c", "3d", "3e", "3f",
            "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "4a", "4b", "4c", "4d", "4e", "4f",
            "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "5a", "5b", "5c", "5d", "5e", "5f",
            "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6a", "6b", "6c", "6d", "6e", "6f",
            "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "7a", "7b", "7c", "7d", "7e", "7f",
            "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "8a", "8b", "8c", "8d", "8e", "8f",
            "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "9a", "9b", "9c", "9d", "9e", "9f",
            "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "aa", "ab", "ac", "ad", "ae", "af",
            "b0", "b1", "b2", "b3", "b4", "b5", "b6", "b7", "b8", "b9", "ba", "bb", "bc", "bd", "be", "bf",
            "c0", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "ca", "cb", "cc", "cd", "ce", "cf",
            "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "da", "db", "dc", "dd", "de", "df",
            "e0", "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "ea", "eb", "ec", "ed", "ee", "ef",
            "f0", "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "fa", "fb", "fc", "fd", "fe", "ff",
    };

    public static void main(String[] arg) throws Exception {
        System.err.println("This utility helps obfuscate passwords to prevent casual observation.");
        System.err.println("It is not securely encrypted and needs further measures to prevent disclosure.");
        if (arg.length != 0 && arg.length != 1) {
            System.err.println("Usage - java  <password>");
            System.err.println("If the password is not specified, the user will be prompted for the password");
            System.exit(1);
        }

        String passwd = arg.length == 1 ? arg[0] : null;
        if (null == passwd) {
            Console console = System.console();

            //read the password, without echoing the output
            char[] password = console.readPassword("Please enter the password: ");

            passwd = new String(password);

            //the javadoc for the Console class recommends "zeroing-out" the password
            //when finished verifying it :
            Arrays.fill(password, ' ');
        }

        System.err.println(obfuscate(passwd));
        System.err.println(encrypt(passwd));
    }

    public static String obfuscate(String s) {
        StringBuilder buf = new StringBuilder();
        byte[] b = s.getBytes();

        buf.append(__OBFUSCATE);
        for (int i = 0; i < b.length; i++) {
            byte b1 = b[i];
            byte b2 = b[s.length() - (i + 1)];
            int i1 = 127 + b1 + b2;
            int i2 = 127 + b1 - b2;
            int i0 = i1 * 256 + i2;
            String x = Integer.toString(i0, 36);

            buf.append(new String(new char[4 - x.length()]).replace("\0", "0"));
            buf.append(x);
        }
        return buf.toString();

    }

    public static String deobfuscate(String s) {
        if (s.startsWith(__OBFUSCATE))
            s = s.substring(__OBFUSCATE.length());

        byte[] b = new byte[s.length() / 2];
        int l = 0;
        for (int i = 0; i < s.length(); i += 4) {
            String x = s.substring(i, i + 4);
            int i0 = Integer.parseInt(x, 36);
            int i1 = (i0 / 256);
            int i2 = (i0 % 256);
            b[l++] = (byte) ((i1 + i2 - 254) / 2);
        }

        return new String(b, 0, l);
    }

    public static String encrypt(String s) throws Exception {
        StringBuilder buf = new StringBuilder(__CRYPT);

        // Instantiate the cipher
        Cipher cipher = Cipher.getInstance(CRYPT_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(NON_SECRET_KEY, CRYPT_ALGORITHM));
        return buf.append(byteArrayToHexString(cipher.doFinal(s.getBytes()))).toString();
    }

    public static String decrypt(String s) throws GeneralSecurityException {
        if (s.startsWith(__CRYPT))
            s = s.substring(__CRYPT.length());

        // Instantiate the cipher
        Cipher cipher = Cipher.getInstance(CRYPT_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(NON_SECRET_KEY, CRYPT_ALGORITHM));
        return new String(cipher.doFinal(hexStringToByteArray(s)));
    }

    public static char[] unfold(String s) throws GeneralSecurityException {
        char[] passwordCopy = null;
        if (null != s) {
            if (s.startsWith(__CRYPT)) {
                passwordCopy = decrypt(s).toCharArray();
            } else if (s.startsWith(__OBFUSCATE)) {
                passwordCopy = deobfuscate(s).toCharArray();
            } else if (s.equalsIgnoreCase(__CONSOLE)) {
                char[] passwordArray = System.console().readPassword("Please enter the password: ");
                passwordCopy = Arrays.copyOf(passwordArray, passwordArray.length);
                Arrays.fill(passwordArray, ' ');
            } else {
                passwordCopy = s.toCharArray();
            }
        }
        return passwordCopy;
    }

    /**
     * @param hex
     * @return
     */
    private static byte[] hexStringToByteArray(String hex) {
        byte rc[] = new byte[hex.length() / 2];
        for (int i = 0; i < rc.length; i++) {
            String h = hex.substring(i * 2, i * 2 + 2);
            int x = Integer.parseInt(h, 16);
            rc[i] = (byte) x;
        }
        return rc;
    }

    /**
     * @param bytes
     * @return
     */
    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder rc = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            rc.append(HEX_TABLE[0xFF & bytes[i]]);
        }
        return rc.toString();
    }
}
