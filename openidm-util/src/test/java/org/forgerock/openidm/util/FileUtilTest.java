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

import java.io.File;
import java.net.URL;
import java.util.Random;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The FileUtilTest does a performance comparison on the read[Large]File implementation.
 *
 */
public class FileUtilTest {

    /*
    mkfile 1k text1k.txt
    mkfile 8k text8k.txt
    mkfile 96k text96k.txt
    mkfile 1m text1m.txt
    mkfile 10m text10m.txt
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        if (null == FileUtilTest.class.getResource("/text1k.txt") ||
                null == FileUtilTest.class.getResource("/text8k.txt") ||
                null == FileUtilTest.class.getResource("/text96k.txt") ||
                null == FileUtilTest.class.getResource("/text1m.txt") ||
                null == FileUtilTest.class.getResource("/text10m.txt")) {
            throw new SkipException("Skipping FileUtil test because the missing test files.");
        }
    }

    @Test(invocationCount = 4)
    public void testReadLargeFile() throws Exception {
        Random random = new Random(9999);
        int iterations = 20;
        long before = System.nanoTime();
        URL testFile = FileUtilTest.class.getResource("/text1k.txt");
        File textFile = new File(testFile.toURI());

        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readLargeFile(textFile);
        }
        System.out.println("1k: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text8k.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readLargeFile(textFile);
        }
        System.out.println("8k: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text96k.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readLargeFile(textFile);
        }
        System.out.println("96k: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text1m.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readLargeFile(textFile);
        }
        System.out.println("1m: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text10m.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readLargeFile(textFile);
        }
        System.out.println("10m: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        System.out.println("End testReadLargeFile");
    }

    private File uncacheFile(Random random, File textFile) {
        //The OS will cache any file that was recently read.
        File newFile = new File(textFile.getParentFile(), random.nextInt() + ".txt");
        textFile.renameTo(newFile);
        textFile = newFile;
        return textFile;
    }

    @Test(invocationCount = 4)
    public void testReadFile() throws Exception {
        Random random = new Random(9999);
        int iterations = 20;
        long before = System.nanoTime();
        URL testFile = FileUtilTest.class.getResource("/text1k.txt");
        File textFile = new File(testFile.toURI());

        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readFile(textFile);
        }
        System.out.println("1k: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text8k.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readFile(textFile);
        }
        System.out.println("8k: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text96k.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readFile(textFile);
        }
        System.out.println("96k: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text1m.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readFile(textFile);
        }
        System.out.println("1m: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        testFile = FileUtilTest.class.getResource("/text10m.txt");
        textFile = new File(testFile.toURI());
        for (int i = 0; i < iterations; i++) {
            textFile = uncacheFile(random, textFile);
            FileUtil.readFile(textFile);
        }
        System.out.println("10m: " + ((System.nanoTime() - before) / 1000L / 1000L) + "ms");
        textFile.renameTo(new File(testFile.toURI()));

        System.out.println("End testReadFile");
    }
}
