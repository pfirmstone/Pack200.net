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

/**
 * Constant pool entry for CONSTANT_Package (tag 20, Java 9+).
 * Structure: tag (1 byte) + name_index (2 bytes, index into UTF8 pool).
 */
public class CPPackage extends ConstantPoolEntry {

    private int index;

    public final String name;

    private final CPUTF8 utf8;

    public CPPackage(CPUTF8 name, int globalIndex) {
        super(ConstantPoolEntry.CP_Package, globalIndex);
        if (name == null) {
            throw new NullPointerException("Null arguments are not allowed");
        }
        this.name = name.underlyingString();
        this.utf8 = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof CPPackage))
            return false;
        return utf8.equals(((CPPackage) obj).utf8);
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return new ClassFileEntry[] { utf8 };
    }

    @Override
    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        index = pool.indexOf(utf8);
    }

    @Override
    public int hashCode() {
        return utf8.hashCode();
    }

    @Override
    protected void writeBody(DataOutputStream dos) throws IOException {
        dos.writeShort(index);
    }

    @Override
    public String toString() {
        return "Package: " + name;
    }
}
