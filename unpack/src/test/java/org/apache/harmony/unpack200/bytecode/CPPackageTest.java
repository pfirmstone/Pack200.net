/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.harmony.unpack200.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import junit.framework.TestCase;

import org.apache.harmony.unpack200.Segment;

/**
 * Tests for {@link CPPackage} (CONSTANT_Package, tag 20).
 */
public class CPPackageTest extends TestCase {

    // -----------------------------------------------------------------------
    // Construction / null guard
    // -----------------------------------------------------------------------

    public void testConstructorSetsName() {
        CPUTF8 utf8 = new CPUTF8("java/lang", 1);
        CPPackage p = new CPPackage(utf8, 5);
        assertEquals("java/lang", p.name);
    }

    public void testConstructorNullThrows() {
        try {
            new CPPackage(null, 0);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // pass
        }
    }

    // -----------------------------------------------------------------------
    // CP tag
    // -----------------------------------------------------------------------

    public void testTag() {
        CPPackage p = new CPPackage(new CPUTF8("java/lang", 1), 0);
        assertEquals(ConstantPoolEntry.CP_Package, p.getTag());
    }

    // -----------------------------------------------------------------------
    // equals / hashCode
    // -----------------------------------------------------------------------

    public void testEqualsSameContent() {
        CPPackage p1 = new CPPackage(new CPUTF8("java/lang", 1), 0);
        CPPackage p2 = new CPPackage(new CPUTF8("java/lang", 1), 77); // globalIndex differs
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    public void testNotEqualsDifferentName() {
        CPPackage p1 = new CPPackage(new CPUTF8("java/lang", 1), 0);
        CPPackage p2 = new CPPackage(new CPUTF8("java/util", 2), 0);
        assertFalse(p1.equals(p2));
    }

    public void testNotEqualsNull() {
        CPPackage p = new CPPackage(new CPUTF8("java/lang", 1), 0);
        assertFalse(p.equals(null));
    }

    public void testNotEqualsDifferentType() {
        CPPackage p = new CPPackage(new CPUTF8("java/lang", 1), 0);
        CPUTF8 utf8 = new CPUTF8("java/lang", 1);
        assertFalse(p.equals(utf8));
    }

    public void testEqualsSelf() {
        CPPackage p = new CPPackage(new CPUTF8("java/lang", 1), 0);
        assertTrue(p.equals(p));
    }

    // -----------------------------------------------------------------------
    // Nested entries
    // -----------------------------------------------------------------------

    public void testNestedClassFileEntriesContainsUTF8() {
        CPUTF8 utf8 = new CPUTF8("java/lang", 1);
        CPPackage p = new CPPackage(utf8, 0);
        ClassFileEntry[] nested = p.getNestedClassFileEntries();
        assertEquals(1, nested.length);
        assertEquals(utf8, nested[0]);
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    public void testToStringContainsName() {
        CPPackage p = new CPPackage(new CPUTF8("java/lang", 1), 0);
        assertTrue(p.toString().contains("java/lang"));
    }

    // -----------------------------------------------------------------------
    // resolve + doWrite (serialised bytes)
    // -----------------------------------------------------------------------

    public void testDoWriteEmitsTagThenNameIndex() throws Exception {
        CPUTF8 utf8 = new CPUTF8("java/lang", 0);
        CPPackage p = new CPPackage(utf8, 1);

        ClassConstantPool pool = new ClassConstantPool();
        pool.add(utf8);
        pool.add(p);
        pool.resolve(new Segment());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        p.doWrite(dos);
        dos.flush();

        byte[] bytes = baos.toByteArray();
        // byte[0]  = tag (20 = CP_Package)
        // byte[1-2] = name_index (unsigned short)
        assertEquals(3, bytes.length);
        assertEquals((byte) 20, bytes[0]);
        int nameIndex = ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF);
        assertTrue("name_index should be > 0 after resolution, was " + nameIndex,
                nameIndex > 0);
    }

    // -----------------------------------------------------------------------
    // ClassConstantPool deduplication
    // -----------------------------------------------------------------------

    public void testDuplicateInConstantPool() {
        CPPackage p1 = new CPPackage(new CPUTF8("java/lang", 1), 0);
        CPPackage p2 = new CPPackage(new CPUTF8("java/lang", 1), 0);

        ClassConstantPool pool = new ClassConstantPool();
        pool.add(p1);
        pool.add(p2);
        pool.addNestedEntries();
        // Only one CPPackage + one CPUTF8 should be present
        assertEquals(2, pool.size());
    }

    // -----------------------------------------------------------------------
    // CPModule and CPPackage must NOT be equal to each other
    // -----------------------------------------------------------------------

    public void testModuleAndPackageNotEqual() {
        CPModule m = new CPModule(new CPUTF8("foo", 1), 0);
        CPPackage p = new CPPackage(new CPUTF8("foo", 1), 0);
        assertFalse("CPModule must not equal CPPackage", m.equals(p));
        assertFalse("CPPackage must not equal CPModule", p.equals(m));
    }
}
