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

import org.apache.harmony.unpack200.SegmentConstantPool;
import org.apache.harmony.unpack200.OperandManager;

/**
 * This class implements the byte code form for the qldc (240) and qldc_w (241)
 * pseudo-opcodes, which encode LDC instructions that reference loadable values
 * such as MethodType and MethodHandle constants (Java 7+).
 *
 * qldc  (240) rewrites to ldc   (18) with a 1-byte CP index.
 * qldc_w(241) rewrites to ldc_w (19) with a 2-byte CP index.
 */
class LoadableValueRefForm extends SingleByteReferenceForm {

    public LoadableValueRefForm(int opcode, String name, int[] rewrite) {
        super(opcode, name, rewrite);
    }

    public LoadableValueRefForm(int opcode, String name, int[] rewrite,
            boolean widened) {
        this(opcode, name, rewrite);
        this.widened = widened;
    }

    protected int getOffset(OperandManager operandManager) {
        return operandManager.nextBcLoadableValueRef();
    }

    protected int getPoolID() {
        return SegmentConstantPool.CP_LOADABLE_VALUE;
    }
}
