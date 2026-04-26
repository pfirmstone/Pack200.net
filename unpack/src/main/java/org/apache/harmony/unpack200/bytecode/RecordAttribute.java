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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Record class file attribute (JVMS §4.7.30, finalized in Java 16).
 *
 * <p>Option A encoding: each component carries its name, field descriptor,
 * and an optional generic signature.  Components that have annotations or
 * other unsupported sub-attributes are not representable and cause the whole
 * class to be passed through uncompressed by the packer.
 *
 * <p>Binary layout written by {@link #writeBody}:
 * <pre>
 *   u2  components_count
 *   for each component:
 *     u2  name_index        (CONSTANT_Utf8)
 *     u2  descriptor_index  (CONSTANT_Utf8)
 *     u2  attributes_count  (0 or 1)
 *     [if signature != null:
 *       u2 attribute_name_index  → "Signature"
 *       u4 attribute_length      → 2
 *       u2 signature_index       (CONSTANT_Utf8)
 *     ]
 * </pre>
 */
public class RecordAttribute extends Attribute {

    private static CPUTF8 ATTRIBUTE_NAME;
    private static CPUTF8 SIGNATURE_ATTRIBUTE_NAME;

    public static void setAttributeName(CPUTF8 cpUTF8Value) {
        ATTRIBUTE_NAME = cpUTF8Value;
    }

    public static void setSignatureAttributeName(CPUTF8 cpUTF8Value) {
        SIGNATURE_ATTRIBUTE_NAME = cpUTF8Value;
    }

    private final int componentsCount;
    private final CPUTF8[] name;
    private final CPUTF8[] descriptor;
    private final CPUTF8[] signature; // element may be null when no Signature sub-attr

    private final int[] nameIndex;
    private final int[] descriptorIndex;
    private final int[] signatureIndex; // 0 when absent
    private int signatureAttrNameIndex; // index of "Signature" in the pool

    /**
     * @param componentsCount number of record components
     * @param name            component names (length == componentsCount)
     * @param descriptor      component field descriptors (length == componentsCount)
     * @param signature       per-component generic signatures; elements may be
     *                        {@code null} when a component has no Signature sub-attribute
     */
    public RecordAttribute(int componentsCount,
                           CPUTF8[] name,
                           CPUTF8[] descriptor,
                           CPUTF8[] signature) {
        super(ATTRIBUTE_NAME);
        this.componentsCount = componentsCount;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.nameIndex = new int[componentsCount];
        this.descriptorIndex = new int[componentsCount];
        this.signatureIndex = new int[componentsCount];
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        List<ClassFileEntry> entries = new ArrayList<ClassFileEntry>();
        entries.add(ATTRIBUTE_NAME);
        boolean anySignature = false;
        for (int i = 0; i < componentsCount; i++) {
            entries.add(name[i]);
            entries.add(descriptor[i]);
            if (signature[i] != null) {
                entries.add(signature[i]);
                anySignature = true;
            }
        }
        if (anySignature) {
            entries.add(SIGNATURE_ATTRIBUTE_NAME);
        }
        return entries.toArray(new ClassFileEntry[0]);
    }

    @Override
    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        boolean anySignature = false;
        for (int i = 0; i < componentsCount; i++) {
            name[i].resolve(pool);
            nameIndex[i] = pool.indexOf(name[i]);
            descriptor[i].resolve(pool);
            descriptorIndex[i] = pool.indexOf(descriptor[i]);
            if (signature[i] != null) {
                signature[i].resolve(pool);
                signatureIndex[i] = pool.indexOf(signature[i]);
                anySignature = true;
            }
        }
        if (anySignature) {
            SIGNATURE_ATTRIBUTE_NAME.resolve(pool);
            signatureAttrNameIndex = pool.indexOf(SIGNATURE_ATTRIBUTE_NAME);
        }
    }

    @Override
    protected int getLength() {
        // u2 components_count
        int length = 2;
        for (int i = 0; i < componentsCount; i++) {
            // u2 name_index + u2 descriptor_index + u2 attributes_count
            length += 6;
            if (signature[i] != null) {
                // u2 attr_name_index + u4 attr_length + u2 signature_index
                length += 8;
            }
        }
        return length;
    }

    @Override
    protected void writeBody(DataOutputStream dos) throws IOException {
        dos.writeShort(componentsCount);
        for (int i = 0; i < componentsCount; i++) {
            dos.writeShort(nameIndex[i]);
            dos.writeShort(descriptorIndex[i]);
            if (signature[i] != null) {
                dos.writeShort(1); // attributes_count
                dos.writeShort(signatureAttrNameIndex);
                dos.writeInt(2); // attribute_length
                dos.writeShort(signatureIndex[i]);
            } else {
                dos.writeShort(0); // attributes_count
            }
        }
    }

    @Override
    public String toString() {
        return "Record";
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        RecordAttribute that = (RecordAttribute) o;
        if (componentsCount != that.componentsCount) return false;
        if (!Arrays.equals(name, that.name)) return false;
        if (!Arrays.equals(descriptor, that.descriptor)) return false;
        return Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + componentsCount;
        hash = 31 * hash + Arrays.hashCode(name);
        hash = 31 * hash + Arrays.hashCode(descriptor);
        hash = 31 * hash + Arrays.hashCode(signature);
        return hash;
    }
}
