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
 * Constant pool entry for a method handle (CONSTANT_MethodHandle).
 * Stores the reference kind and the referenced field/method/interface-method.
 */
class CPMethodHandle extends ConstantPoolEntry implements Comparable {

    private final int refkind;
    private final CPMethodOrField member;
    /** true when the member lives in cp_Field (refkind 1-4) */
    private final boolean isField;
    /** true when the member lives in cp_Imethod (refkind 9) */
    private final boolean isIMethod;

    CPMethodHandle(int refkind, CPMethodOrField member,
                   boolean isField, boolean isIMethod) {
        this.refkind = refkind;
        this.member = member;
        this.isField = isField;
        this.isIMethod = isIMethod;
    }

    public int getRefkind() { return refkind; }
    public CPMethodOrField getMember() { return member; }
    public boolean isField() { return isField; }
    public boolean isIMethod() { return isIMethod; }

    @Override
    public int compareTo(Object o) {
        CPMethodHandle that = (CPMethodHandle) o;
        if (refkind != that.refkind) return refkind - that.refkind;
        return member.compareTo(that.member);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof CPMethodHandle)) return false;
        CPMethodHandle that = (CPMethodHandle) obj;
        return refkind == that.refkind && member.equals(that.member);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + refkind;
        hash = 31 * hash + (member != null ? member.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "MethodHandle:" + refkind + " " + member;
    }
}
