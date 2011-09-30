/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.crypto.impl;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.Console;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class Main {

    public static final String __OBFUSCATE = "OBF:";
    public static final String __CRYPT = "CRYPT:";
    public static final String __CONSOLE = "CONSOLE";

    private static final String CRYPT_ALGORITHM = "AES";

    private static final byte[] NON_SECRET_KEY = {-50, 50, -16, -26, -99, -61, 94, 45, 26, 75, -39, -44, -48, -75, 40, 4};


    public static void main(String[] arg) throws Exception {
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

            switch (x.length()) {
                case 1:
                    buf.append('0');
                case 2:
                    buf.append('0');
                case 3:
                    buf.append('0');
                default:
                    buf.append(x);
            }
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


    private static final byte[] HEXES = "0123456789ABCDEF".getBytes();

    private static String byteArrayToHexString(final byte[] raw) {
        if (raw == null) return null;
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES[(b >> 4) & 0xF]).append(HEXES[(b) & 0xF]);
        }
        return hex.toString();
    }

    private static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
}
