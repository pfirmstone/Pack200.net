/*
 * Copyright 2018 peter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.harmony.pack200;

import java.util.Arrays;

/**
 * Constant pool band entry for a bootstrap method (cp_BootstrapMethod).
 * Stores the method handle that acts as the bootstrap and its static arguments
 * (loadable values).
 */
class CPBootstrapMethod extends ConstantPoolEntry implements Comparable {

    private final CPMethodHandle methodHandle;
    /** Each element is a CP entry that is a loadable value. */
    private final ConstantPoolEntry[] args;

    CPBootstrapMethod(CPMethodHandle methodHandle, ConstantPoolEntry[] args) {
        this.methodHandle = methodHandle;
        this.args = args;
    }

    public CPMethodHandle getMethodHandle() { return methodHandle; }
    public ConstantPoolEntry[] getArgs() { return args; }

    @Override
    public int compareTo(Object o) {
        CPBootstrapMethod that = (CPBootstrapMethod) o;
        int cmp = methodHandle.compareTo(that.methodHandle);
        if (cmp != 0) return cmp;
        if (args.length != that.args.length) return args.length - that.args.length;
        for (int i = 0; i < args.length; i++) {
            cmp = ((Comparable) args[i]).compareTo(that.args[i]);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof CPBootstrapMethod)) return false;
        CPBootstrapMethod that = (CPBootstrapMethod) obj;
        return methodHandle.equals(that.methodHandle)
                && Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (methodHandle != null ? methodHandle.hashCode() : 0);
        hash = 31 * hash + Arrays.hashCode(args);
        return hash;
    }

    @Override
    public String toString() {
        return "BootstrapMethod:" + methodHandle + " " + Arrays.toString(args);
    }
}
