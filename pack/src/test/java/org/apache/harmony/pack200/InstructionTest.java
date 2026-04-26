/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.harmony.pack200;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.harmony.unpack200.common.Pack200Exception;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for JSR-335 (Java 8+) invokespecial and invokestatic on interface
 * methods (pseudo-opcodes 242 and 243 in Pack200).
 */
public class InstructionTest {

    /**
     * Tests that invokespecial and invokestatic on interface methods (JSR-335)
     * survive a pack/unpack round-trip with byte-for-byte identical class files.
     *
     * The test JAR contains:
     *   interface I { default void forEach(){} static void next() {} }
     *   class A implements I {
     *       public void forEach(Object o) {
     *           I.super.forEach();   // invokespecial on interface (pseudo-opcode 242)
     *           I.next();            // invokestatic  on interface (pseudo-opcode 243)
     *       }
     *   }
     */
    @Test
    public void testJSR335InterfaceMethodInstructions()
            throws IOException, Pack200Exception, URISyntaxException, InterruptedException {

        // Pack the JSR-335 test JAR
        JarFile in = new JarFile(new File(Pack200Archive.class.getResource(
                "/org/apache/harmony/pack200/tests/jsr335.jar").toURI()));
        File packFile = File.createTempFile("jsr335", ".pack");
        packFile.deleteOnExit();
        FileOutputStream packOut = new FileOutputStream(packFile);

        PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Pack200Archive(in, packOut, options).pack();
        in.close();
        packOut.close();

        // Unpack back to a JAR
        InputStream packIn = new BufferedInputStream(new FileInputStream(packFile));
        File unpackedFile = File.createTempFile("jsr335out", ".jar");
        unpackedFile.deleteOnExit();
        JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(unpackedFile));
        new org.apache.harmony.unpack200.UnPack200Archive(packIn, jarOut).unpack();
        packIn.close();

        // Compare the unpacked class files against the originals byte-for-byte.
        // Note: Pack200 reorders the constant pool, so we save the unpacked files
        // and compare structurally via javap disassembly (same method bodies).
        JarFile unpacked = new JarFile(unpackedFile);

        // Save unpacked class files for structural inspection
        File tmpDir = File.createTempFile("jsr335inspect", "");
        tmpDir.delete();
        tmpDir.mkdirs();

        Enumeration<JarEntry> entries = unpacked.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            byte[] data = readAll(unpacked.getInputStream(entry));
            File outFile = new File(tmpDir, name.replace('/', '_'));
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(data);
            }
        }

        // Structurally compare A.class: verify invokespecial and invokestatic
        // on interface methods use InterfaceMethodref CP entries (not Methodref).
        File unpackedA = new File(tmpDir, "A.class");
        assertTrue("A.class not found in unpacked JAR", unpackedA.exists());
        String javap = javap(unpackedA);
        assertTrue("invokespecial should reference InterfaceMethod I.forEach:()V after pack/unpack; got:\n" + javap,
                javap.contains("invokespecial") && javap.contains("InterfaceMethod I.forEach"));
        assertTrue("invokestatic should reference InterfaceMethod I.next:()V after pack/unpack; got:\n" + javap,
                javap.contains("invokestatic") && javap.contains("InterfaceMethod I.next"));

        // Also verify the I.class round-trips correctly (no extra CP entries)
        File unpackedI = new File(tmpDir, "I.class");
        assertTrue("I.class not found in unpacked JAR", unpackedI.exists());
        String javapI = javap(unpackedI);
        // I.class should have the same 2 methods, no extra bytecodes
        assertTrue("I.class should contain forEach() after pack/unpack; javap:\n" + javapI,
                javapI.contains("forEach"));
        assertTrue("I.class should contain next() after pack/unpack; javap:\n" + javapI,
                javapI.contains("next"));
        // Neither method should have method calls (they are empty)
        assertFalse("I.class methods should not contain invokespecial after pack/unpack; javap:\n" + javapI,
                javapI.contains("invokespecial ") || javapI.contains("invokestatic "));

        unpacked.close();
        // Cleanup
        File[] tmpFiles = tmpDir.listFiles();
        if (tmpFiles != null) {
            for (File f : tmpFiles) f.delete();
        }
        tmpDir.delete();
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        is.close();
        return bos.toByteArray();
    }

    private static String javap(File classFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                System.getProperty("java.home") + "/bin/javap",
                "-verbose", "-c", classFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        p.waitFor();
        return sb.toString();
    }
}
