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
 * This class implements the byte code form for Pack200 pseudo-opcodes
 * invokespecial_interface (242) and invokestatic_interface (243).
 * <p>
 * These pseudo-opcodes represent {@code invokespecial} and {@code invokestatic}
 * instructions whose owner class is an interface (JSR-335 / Java 8+).  Like
 * {@code invokeinterface}, they reference a
 * {@code CONSTANT_InterfaceMethodref} entry from the {@code cp_Imethod} pool.
 * Unlike {@code invokeinterface}, they encode as plain 3-byte instructions
 * (opcode + 2-byte constant-pool index) and carry no count byte.
 */
class InterfaceMethodRefForm extends ReferenceForm {

    public InterfaceMethodRefForm(int opcode, String name, int[] rewrite) {
        super(opcode, name, rewrite);
    }

    protected int getOffset(OperandManager operandManager) {
        return operandManager.nextIMethodRef();
    }

    protected int getPoolID() {
        return SegmentConstantPool.CP_IMETHOD;
    }
}
