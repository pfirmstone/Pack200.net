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
 * Tests for {@link CPModule} (CONSTANT_Module, tag 19).
 */
public class CPModuleTest extends TestCase {

    // -----------------------------------------------------------------------
    // Construction / null guard
    // -----------------------------------------------------------------------

    public void testConstructorSetsName() {
        CPUTF8 utf8 = new CPUTF8("java.base", 1);
        CPModule m = new CPModule(utf8, 5);
        assertEquals("java.base", m.name);
    }

    public void testConstructorNullThrows() {
        try {
            new CPModule(null, 0);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // pass
        }
    }

    // -----------------------------------------------------------------------
    // CP tag
    // -----------------------------------------------------------------------

    public void testTag() {
        CPModule m = new CPModule(new CPUTF8("java.base", 1), 0);
        assertEquals(ConstantPoolEntry.CP_Module, m.getTag());
    }

    // -----------------------------------------------------------------------
    // equals / hashCode
    // -----------------------------------------------------------------------

    public void testEqualsSameContent() {
        CPModule m1 = new CPModule(new CPUTF8("java.base", 1), 0);
        CPModule m2 = new CPModule(new CPUTF8("java.base", 1), 99); // globalIndex differs
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    public void testNotEqualsDifferentName() {
        CPModule m1 = new CPModule(new CPUTF8("java.base", 1), 0);
        CPModule m2 = new CPModule(new CPUTF8("java.se", 2), 0);
        assertFalse(m1.equals(m2));
    }

    public void testNotEqualsNull() {
        CPModule m = new CPModule(new CPUTF8("java.base", 1), 0);
        assertFalse(m.equals(null));
    }

    public void testNotEqualsDifferentType() {
        CPModule m = new CPModule(new CPUTF8("java.base", 1), 0);
        CPUTF8 utf8 = new CPUTF8("java.base", 1);
        assertFalse(m.equals(utf8));
    }

    public void testEqualsSelf() {
        CPModule m = new CPModule(new CPUTF8("java.base", 1), 0);
        assertTrue(m.equals(m));
    }

    // -----------------------------------------------------------------------
    // Nested entries
    // -----------------------------------------------------------------------

    public void testNestedClassFileEntriesContainsUTF8() {
        CPUTF8 utf8 = new CPUTF8("java.base", 1);
        CPModule m = new CPModule(utf8, 0);
        ClassFileEntry[] nested = m.getNestedClassFileEntries();
        assertEquals(1, nested.length);
        assertEquals(utf8, nested[0]);
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    public void testToStringContainsName() {
        CPModule m = new CPModule(new CPUTF8("java.base", 1), 0);
        assertTrue(m.toString().contains("java.base"));
    }

    // -----------------------------------------------------------------------
    // resolve + doWrite (serialised bytes)
    // -----------------------------------------------------------------------

    public void testDoWriteEmitsTagThenNameIndex() throws Exception {
        CPUTF8 utf8 = new CPUTF8("java.base", 0);
        CPModule m = new CPModule(utf8, 1);

        // Build a minimal constant pool so resolve() can look up the utf8 index.
        ClassConstantPool pool = new ClassConstantPool();
        pool.add(utf8);
        pool.add(m);
        pool.resolve(new Segment());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        m.doWrite(dos);
        dos.flush();

        byte[] bytes = baos.toByteArray();
        // byte[0]  = tag (19 = CP_Module)
        // byte[1-2] = name_index (unsigned short)
        assertEquals(3, bytes.length);
        assertEquals((byte) 19, bytes[0]);
        // name_index must be positive (at least 1) after pool resolution
        int nameIndex = ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF);
        assertTrue("name_index should be > 0 after resolution, was " + nameIndex,
                nameIndex > 0);
    }

    // -----------------------------------------------------------------------
    // ClassConstantPool deduplication
    // -----------------------------------------------------------------------

    public void testDuplicateInConstantPool() {
        CPModule m1 = new CPModule(new CPUTF8("java.base", 1), 0);
        CPModule m2 = new CPModule(new CPUTF8("java.base", 1), 0);

        ClassConstantPool pool = new ClassConstantPool();
        pool.add(m1);
        pool.add(m2);
        pool.addNestedEntries();
        // Only one CPModule + one CPUTF8 should be present
        assertEquals(2, pool.size());
    }
}
