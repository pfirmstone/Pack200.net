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
 * Tests for {@link CPDynamic} (CONSTANT_Dynamic, tag 17).
 */
public class CPDynamicTest extends TestCase {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Build a minimal CPBootstrapMethod using a REF_getField (kind=1) handle. */
    private static CPBootstrapMethod buildBootstrap(int bsmAttrIndex) {
        CPUTF8 className = new CPUTF8("Owner", 1);
        CPUTF8 fieldName = new CPUTF8("val", 2);
        CPUTF8 fieldDesc = new CPUTF8("I", 3);
        CPClass cpClass = new CPClass(className, 10);
        CPNameAndType nat = new CPNameAndType(fieldName, fieldDesc, 11);
        CPFieldRef fieldRef = new CPFieldRef(cpClass, nat, 12);
        CPMethodHandle handle = new CPMethodHandle(1, fieldRef, 13);
        return new CPBootstrapMethod(handle, 0, new CPLoadableValue[0], bsmAttrIndex);
    }

    /** Build a CPNameAndType for use in CPDynamic. */
    private static CPNameAndType buildNameAndType() {
        return new CPNameAndType(new CPUTF8("myConst", 5), new CPUTF8("Ljava/lang/String;", 6), 20);
    }

    // -----------------------------------------------------------------------
    // CP tag
    // -----------------------------------------------------------------------

    public void testTag() {
        CPDynamic d = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);
        assertEquals(ConstantPoolEntry.CP_Dynamic, d.getTag());
    }

    // -----------------------------------------------------------------------
    // equals / hashCode
    // -----------------------------------------------------------------------

    public void testEqualsSameContent() {
        CPDynamic d1 = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);
        CPDynamic d2 = new CPDynamic(buildBootstrap(0), buildNameAndType(), 99); // globalIndex differs
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    public void testNotEqualsDifferentBootstrap() {
        CPDynamic d1 = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);

        // Use a different class name so the bootstrap method handle differs
        CPUTF8 className2 = new CPUTF8("OtherOwner", 1);
        CPUTF8 fieldName2 = new CPUTF8("val", 2);
        CPUTF8 fieldDesc2 = new CPUTF8("I", 3);
        CPClass cpClass2 = new CPClass(className2, 10);
        CPNameAndType nat2 = new CPNameAndType(fieldName2, fieldDesc2, 11);
        CPFieldRef fieldRef2 = new CPFieldRef(cpClass2, nat2, 12);
        CPMethodHandle handle2 = new CPMethodHandle(1, fieldRef2, 13);
        CPBootstrapMethod bsm2 = new CPBootstrapMethod(handle2, 0, new CPLoadableValue[0], 0);

        CPDynamic d2 = new CPDynamic(bsm2, buildNameAndType(), 0);
        assertFalse(d1.equals(d2));
    }

    public void testNotEqualsDifferentNameAndType() {
        CPDynamic d1 = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);
        CPNameAndType nat2 = new CPNameAndType(new CPUTF8("other", 5), new CPUTF8("I", 6), 20);
        CPDynamic d2 = new CPDynamic(buildBootstrap(0), nat2, 0);
        assertFalse(d1.equals(d2));
    }

    public void testNotEqualsNull() {
        CPDynamic d = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);
        assertFalse(d.equals(null));
    }

    public void testNotEqualsDifferentType() {
        CPDynamic d = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);
        assertFalse(d.equals(new CPUTF8("x", 1)));
    }

    public void testEqualsSelf() {
        CPDynamic d = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);
        assertTrue(d.equals(d));
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    public void testToStringContainsCpDynamic() {
        CPDynamic d = new CPDynamic(buildBootstrap(0), buildNameAndType(), 0);
        assertTrue(d.toString().startsWith("cp_Dynamic"));
    }

    // -----------------------------------------------------------------------
    // CPDynamic must NOT equal CPInvokeDynamic even for identical structure
    // -----------------------------------------------------------------------

    public void testNotEqualToInvokeDynamic() {
        CPBootstrapMethod bsm = buildBootstrap(0);
        CPNameAndType nat = buildNameAndType();
        CPDynamic dynamic = new CPDynamic(bsm, nat, 0);
        CPInvokeDynamic invokeDynamic = new CPInvokeDynamic(bsm, nat, 0);
        assertFalse("CPDynamic must not equal CPInvokeDynamic",
                dynamic.equals(invokeDynamic));
        assertFalse("CPInvokeDynamic must not equal CPDynamic",
                invokeDynamic.equals(dynamic));
    }

    // -----------------------------------------------------------------------
    // resolve + doWrite (serialised bytes)
    // -----------------------------------------------------------------------

    public void testDoWriteEmitsTagThenBsmIndexThenNatIndex() throws Exception {
        // bootstrap_method_attr_index is fixed at construction time (bsmAttrIndex param)
        // name_and_type_index comes from ClassConstantPool resolution
        CPBootstrapMethod bsm = buildBootstrap(3); // bootstrap_method_attr_index = 3
        CPNameAndType nat = buildNameAndType();
        CPDynamic d = new CPDynamic(bsm, nat, 0);

        // We add enough entries so the pool can resolve nat's index
        ClassConstantPool pool = new ClassConstantPool();
        // add nat's constituents so pool can resolve them
        pool.add(new CPUTF8("myConst", 5));
        pool.add(new CPUTF8("Ljava/lang/String;", 6));
        pool.add(nat);
        pool.resolve(new Segment());

        // Trigger resolve on the CPDynamic directly (pool is already resolved)
        d.resolve(pool);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        d.doWrite(dos);
        dos.flush();

        byte[] bytes = baos.toByteArray();
        // byte[0]   = tag (17 = CP_Dynamic)
        // byte[1-2] = bootstrap_method_attr_index (unsigned short) = 3
        // byte[3-4] = name_and_type_index (unsigned short)
        assertEquals(5, bytes.length);
        assertEquals((byte) 17, bytes[0]);
        int bsmIdx = ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF);
        assertEquals(3, bsmIdx);
        int natIdx = ((bytes[3] & 0xFF) << 8) | (bytes[4] & 0xFF);
        assertTrue("name_and_type_index should be > 0 after resolution, was " + natIdx,
                natIdx > 0);
    }
}
