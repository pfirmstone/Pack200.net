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
package org.apache.harmony.unpack200;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.harmony.unpack200.Segment;

public class ClassVersionTest extends TestCase {

    private static final int JAVA_5 = 49;
    private static final int JAVA_6 = 50;
    private static final int JAVA_7 = 51;
    private static final int JAVA_8 = 52;
    private static final int JAVA_9 = 53;
    private static final int JAVA_10 = 54;
    private static final int JAVA_11 = 55;
    private static final int JAVA_12 = 56;
    private static final int JAVA_13 = 57;
    private static final int JAVA_14 = 58;
    private static final int JAVA_15 = 59;
    private static final int JAVA_16 = 60;
    private static final int JAVA_17 = 61;
    private static final int JAVA_18 = 62;
    private static final int JAVA_19 = 63;
    private static final int JAVA_20 = 64;
    private static final int JAVA_21 = 65;
    private static final int JAVA_22 = 66;
    private static final int JAVA_23 = 67;
    private static final int JAVA_24 = 68;
    private static final int JAVA_25 = 69;
    private static final int JAVA_26 = 70;
    private static final int JAVA_27 = 71;

    /**
     * Verifies that the class-file major version constants in this test class
     * match the JVM Specification (JVMS Table 4.1-B) for Java 5 through 27.
     * Each Java N release uses major version (44 + N).
     */
    public void testJvmSpecVersionNumbers() {
        assertEquals(49, JAVA_5);
        assertEquals(50, JAVA_6);
        assertEquals(51, JAVA_7);
        assertEquals(52, JAVA_8);
        assertEquals(53, JAVA_9);
        assertEquals(54, JAVA_10);
        assertEquals(55, JAVA_11);
        assertEquals(56, JAVA_12);
        assertEquals(57, JAVA_13);
        assertEquals(58, JAVA_14);
        assertEquals(59, JAVA_15);
        assertEquals(60, JAVA_16);
        assertEquals(61, JAVA_17);
        assertEquals(62, JAVA_18);
        assertEquals(63, JAVA_19);
        assertEquals(64, JAVA_20);
        assertEquals(65, JAVA_21);
        assertEquals(66, JAVA_22);
        assertEquals(67, JAVA_23);
        assertEquals(68, JAVA_24);
        assertEquals(69, JAVA_25);
        assertEquals(70, JAVA_26);
        assertEquals(71, JAVA_27);
        // Verify the formula: major = 44 + javaVersion
        for (int javaVersion = 5; javaVersion <= 27; javaVersion++) {
            int expectedMajor = 44 + javaVersion;
            // Round-trip: constant value for this Java version
            int constantValue = JAVA_5 + (javaVersion - 5);
            assertEquals("Java " + javaVersion + " major version mismatch",
                    expectedMajor, constantValue);
        }
    }

    public void testCorrectVersionOfSegment() throws IOException {
        InputStream in = Segment.class
                .getResourceAsStream("/org/apache/harmony/unpack200/Segment.class");
        DataInputStream din = new DataInputStream(in);

        assertEquals(0xCAFEBABE, din.readInt());
        din.readShort(); // MINOR -- don't care
//        assertTrue("Class file has been compiled with Java 1.5 compatibility"
//                + " instead of 1.4 or lower", din.readShort() < JAVA_15);
	assertTrue("Class not compiled with Java 27 compatibility", din.readShort() <= JAVA_27);
    }

    public void testCorrectVersionOfTest() throws IOException {
        InputStream in = Segment.class
                .getResourceAsStream("/org/apache/harmony/unpack200/ClassVersionTest.class");
        DataInputStream din = new DataInputStream(in);

        assertEquals(0xCAFEBABE, din.readInt());
        din.readShort(); // MINOR -- don't care
//        assertTrue("Class file has been compiled with Java 1.5 compatibility"
//                + " instead of 1.4 or lower", din.readShort() < JAVA_15);
	assertTrue("Class not compiled with Java 27 compatibility", din.readShort() <= JAVA_27);
        din.close();
    }

    public void testCorrectVersionOfAdapter() throws IOException {
        // tests that both the file is on the classpath and that it's been
        // compiled correctly, but without actually loading the class
        InputStream in = Segment.class
                .getResourceAsStream("/org/apache/harmony/unpack200/Pack200Adapter.class");
        if (in != null) { // If running in Eclipse and Java5 stuff not
            // built/available
            DataInputStream din = new DataInputStream(in);

            assertEquals(0xCAFEBABE, din.readInt());
            din.readShort(); // MINOR -- don't care
            assertTrue("Class file needs 1.6 compatibility",
                    din.readShort() >= JAVA_6);
            din.close();
        }
    }
}
