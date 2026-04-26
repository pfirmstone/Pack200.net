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

/**
 * Constant pool entry for an invokedynamic call site (CONSTANT_InvokeDynamic).
 * Stores the bootstrap method and the name-and-type of the dynamic call site.
 */
class CPInvokeDynamic extends ConstantPoolEntry implements Comparable {

    private final CPBootstrapMethod bootstrapMethod;
    private final CPNameAndType nameAndType;

    CPInvokeDynamic(CPBootstrapMethod bootstrapMethod, CPNameAndType nameAndType) {
        this.bootstrapMethod = bootstrapMethod;
        this.nameAndType = nameAndType;
    }

    public CPBootstrapMethod getBootstrapMethod() { return bootstrapMethod; }
    public CPNameAndType getNameAndType() { return nameAndType; }

    @Override
    public int compareTo(Object o) {
        CPInvokeDynamic that = (CPInvokeDynamic) o;
        int cmp = bootstrapMethod.compareTo(that.bootstrapMethod);
        if (cmp != 0) return cmp;
        return nameAndType.compareTo(that.nameAndType);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof CPInvokeDynamic)) return false;
        CPInvokeDynamic that = (CPInvokeDynamic) obj;
        return bootstrapMethod.equals(that.bootstrapMethod)
                && nameAndType.equals(that.nameAndType);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (bootstrapMethod != null ? bootstrapMethod.hashCode() : 0);
        hash = 31 * hash + (nameAndType != null ? nameAndType.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "InvokeDynamic:" + bootstrapMethod + " " + nameAndType;
    }
}
