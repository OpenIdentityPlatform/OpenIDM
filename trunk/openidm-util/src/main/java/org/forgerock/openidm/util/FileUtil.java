/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * The FileUtil class contains common methods to read text files.
 *
 * @author Laszlo Hordos
 */
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * Read large > 5Mb text files to String.
     *
     * @param file
     *            source file
     * @return content of the source {@code file}
     * @throws IOException
     *             when the source {@code file} can not be read
     */
    public final static String readLargeFile(File file) throws IOException {
        FileChannel channel = new FileInputStream(file).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
        channel.read(buffer);
        channel.close();
        return new String(buffer.array());
    }

    /**
     * Read small < 5Mb text files to String.
     *
     * @param file
     *            source file
     * @return content of the source {@code file}
     * @throws IOException
     *             when the source {@code file} can not be read
     */
    public static final String readFile(File file) throws IOException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            SimpleByteBuffer buffer = new SimpleByteBuffer();
            /*
             * if you are reading really large files you might want to up the
             * buffer from 1024 to a max of 8192.
             */
            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) != -1) {
                buffer.put(buf, len);
            }
            return new String(buffer.buffer, 0, buffer.write);
        } finally {
            if (null != in) {
                in.close();
            }
        }
    }

    static class SimpleByteBuffer {

        private byte[] buffer = new byte[256];

        private int write;

        public void put(byte[] buf, int len) {
            ensure(len);
            System.arraycopy(buf, 0, buffer, write, len);
            write += len;
        }

        private void ensure(int amt) {
            int req = write + amt;
            if (buffer.length <= req) {
                byte[] temp = new byte[req * 2];
                System.arraycopy(buffer, 0, temp, 0, write);
                buffer = temp;
            }
        }

    }
}
