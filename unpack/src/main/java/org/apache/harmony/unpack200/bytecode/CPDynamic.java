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
 * Constant pool entry for CONSTANT_Dynamic (tag 17, Java 11+).
 * Structure: tag (1 byte) + bootstrap_method_attr_index (2 bytes) +
 * name_and_type_index (2 bytes).  Identical structure to CONSTANT_InvokeDynamic
 * but represents a dynamically-computed constant rather than a call site.
 * CONSTANT_Dynamic is a loadable value and may appear as an ldc argument.
 */
public class CPDynamic extends CPLoadableValue {

    private final CPBootstrapMethod cpBootstrapMethodValue;
    private final CPNameAndType cpNameAndTypeValue;

    private int bootstrap_method_attr_index;
    private int cpNameAndTypeIndex;

    public CPDynamic(CPBootstrapMethod cpBootstrapMethodValue,
            CPNameAndType cpNameAndTypeValue, int globalIndex) {
        super(ConstantPoolEntry.CP_Dynamic, globalIndex);
        this.cpBootstrapMethodValue = cpBootstrapMethodValue;
        this.cpNameAndTypeValue = cpNameAndTypeValue;
    }

    @Override
    protected void resolve(ClassConstantPool pool) {
        bootstrap_method_attr_index = cpBootstrapMethodValue.getBootstrapMethodAttrIndex();
        cpNameAndTypeIndex = pool.indexOf(cpNameAndTypeValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof CPDynamic)) return false;
        CPDynamic that = (CPDynamic) obj;
        if (!cpBootstrapMethodValue.equals(that.cpBootstrapMethodValue)) return false;
        return cpNameAndTypeValue.equals(that.cpNameAndTypeValue);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (cpBootstrapMethodValue != null ? cpBootstrapMethodValue.hashCode() : 0);
        hash = 53 * hash + (cpNameAndTypeValue != null ? cpNameAndTypeValue.hashCode() : 0);
        return hash;
    }

    @Override
    protected void writeBody(DataOutputStream dos) throws IOException {
        dos.writeShort(bootstrap_method_attr_index);
        dos.writeShort(cpNameAndTypeIndex);
    }

    @Override
    public String toString() {
        return "cp_Dynamic " + cpBootstrapMethodValue + " " + cpNameAndTypeValue;
    }
}
