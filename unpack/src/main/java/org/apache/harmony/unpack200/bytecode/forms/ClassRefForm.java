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
package org.apache.harmony.unpack200.bytecode.forms;

import org.apache.harmony.unpack200.common.Pack200Exception;
import org.apache.harmony.unpack200.SegmentConstantPool;
import org.apache.harmony.unpack200.bytecode.ByteCode;
import org.apache.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.harmony.unpack200.OperandManager;

/**
 * This class implements the byte code form for those bytecodes which have class
 * references (and only class references).
 */
class ClassRefForm extends ReferenceForm {

    protected boolean widened;

    public ClassRefForm(int opcode, String name, int[] rewrite) {
        super(opcode, name, rewrite);
    }

    public ClassRefForm(int opcode, String name, int[] rewrite, boolean widened) {
        this(opcode, name, rewrite);
        this.widened = widened;
    }

    protected void setNestedEntries(ByteCode byteCode,
            OperandManager operandManager, int offset) throws Pack200Exception {
        // If the offset is not zero, proceed normally.
        if (offset != 0) {
            super.setNestedEntries(byteCode, operandManager, offset - 1);
            return;
        }
        // If the offset is 0, ClassRefForms refer to
        // the current class. Add that as the nested class.
        // (This is true for all bc_classref forms in
        // the spec except for multianewarray, which has
        // its own form.)
        final SegmentConstantPool globalPool = operandManager
                .globalConstantPool();
        ClassFileEntry[] nested = null;
        // How do I get this class?
        nested = new ClassFileEntry[] { globalPool
                .getClassPoolEntry(operandManager.getCurrentClass()) };
        byteCode.setNested(nested);
        byteCode.setNestedPositions(new int[][] { { 0, 2 } });
    }

    protected int getOffset(OperandManager operandManager) {
        return operandManager.nextClassRef();
    }

    protected int getPoolID() {
        return SegmentConstantPool.CP_CLASS;
    }
}
