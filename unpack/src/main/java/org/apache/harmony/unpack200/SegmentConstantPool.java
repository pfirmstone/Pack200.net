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
package org.apache.harmony.unpack200;

import java.util.List;

import org.apache.harmony.unpack200.common.Pack200Exception;
import org.apache.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.harmony.unpack200.bytecode.ConstantPoolEntry;

/**
 * SegmentConstantPool manages the constant pool used for re-creating class
 * files.
 */
public class SegmentConstantPool {

    private final CpBands bands;
    private final SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();
    /**
     * @param bands
     */
    public SegmentConstantPool(CpBands bands) {
        this.bands = bands;
    }

    // define in archive order
    // Names defined in JSR 200 and Pack 200 Standards
    public static final int ALL = 0;
    public static final int UTF_8 = 1; // CONSTANT_Utf8
    public static final int CP_INT = 2; // CONSTANT_Integer
    public static final int CP_FLOAT = 3; // CONSTANT_Float
    public static final int CP_LONG = 4; // CONSTANT_Long
    public static final int CP_DOUBLE = 5; // CONSTANT_Double
    public static final int CP_STRING = 6; // CONSTANT_String
    public static final int CP_CLASS = 7; // CONSTANT_Class
    public static final int SIGNATURE = 8; // (none)
    public static final int CP_DESCR = 9; // CONSTANT_NameAndType
    public static final int CP_FIELD = 10; // CONSTANT_Fieldref
    public static final int CP_METHOD = 11; // CONSTANT_Methodref
    public static final int CP_IMETHOD = 12; // CONSTANT_InterfaceMethodref
    public static final int CP_METHOD_HANDLE = 13; // CONSTANT_MethodHandle
    public static final int CP_METHOD_TYPE = 14; // CONSTANT_MethodType
    public static final int CP_BOOTSTRAP_METHOD = 15; // (none; side table to constant pool)  class filed elements are attribute.bootstrap_methods[i]
    public static final int CP_INVOKE_DYNAMIC = 16; // CONSTANT_InvokeDynamic
    // Java 9
    public static final int CP_MODULE = 17; // CONSTANT_Module
    public static final int CP_PACKAGE = 18; // CONSTANT_Package
    // Java 11
    public static final int CP_DYNAMIC = 19; // CONSTANT_Dynamic
    // Java 12 JDK-8161256 Informational only, unreleased
    // CP_GROUP
    // CP_BYTES
    
    // Negative numbers represent groups
    public static final int CP_LOADABLE_VALUE = -2;
    public static final int CP_ANY_MEMBER = -3;

    protected static final String REGEX_MATCH_ALL = ".*";
    protected static final String INITSTRING = "<init>";
    protected static final String REGEX_MATCH_INIT = "^" + INITSTRING + ".*";

    public ClassFileEntry getValue(int cp, long value) throws Pack200Exception {
        int index = (int) value;
	if (index == -1) return null;
	if (index < -1) throw new Pack200Exception("Cannot have a negative range");
	switch (cp){
	    case UTF_8:
		return bands.cpUTF8Value(index);
	    case CP_INT:
		return bands.cpIntegerValue(index);
	    case CP_FLOAT:
		return bands.cpFloatValue(index);
	    case CP_LONG:
		return bands.cpLongValue(index);
	    case CP_DOUBLE:
		return bands.cpDoubleValue(index);
	    case CP_STRING:
		return bands.cpStringValue(index);
	    case CP_CLASS:
		return bands.cpClassValue(index);
	    case SIGNATURE:
		return bands.cpSignatureValue(index);
	    case CP_DESCR:
		return bands.cpNameAndTypeValue(index);
	    case CP_METHOD_HANDLE:
		return bands.cpMethodHandleValue(index);
	    case CP_METHOD_TYPE:
		return bands.cpMethodTypeValue(index);
	    case CP_BOOTSTRAP_METHOD:
		return bands.cpBootstrapMethodValue(index);
	    case CP_INVOKE_DYNAMIC:
		return bands.cpInvokeDynamicValue(index);
	    case CP_MODULE:
		return bands.cpModuleValue(index);
	    case CP_PACKAGE:
		return bands.cpPackageValue(index);
	    case CP_DYNAMIC:
		return bands.cpDynamicValue(index);
	    case CP_LOADABLE_VALUE:
		return bands.cpLoadableValue(index);
	    case CP_ANY_MEMBER:
		return bands.cpAnyMemberValue(index);
	    case ALL:
		return bands.all(index);
	    default:
		throw new Error("Tried to get a value I don't know about: " + cp);
	}
    }

    /**
     * Subset the constant pool of the specified type to be just that which has
     * the specified class name. Answer the ConstantPoolEntry at the
     * desiredIndex of the subsetted pool.
     *
     * @param cp
     *            type of constant pool array to search
     * @param desiredIndex
     *            index of the constant pool
     * @param desiredClassName
     *            class to use to generate a subset of the pool
     * @return ConstantPoolEntry
     * @throws Pack200Exception
     */
    public ConstantPoolEntry getClassSpecificPoolEntry(int cp,
            long desiredIndex, String desiredClassName) throws Pack200Exception {
        int index = (int) desiredIndex;
        int realIndex = -1;
        String array[] = null;
        if (cp == CP_FIELD) {
            array = bands.getCpFieldClass();
        } else if (cp == CP_METHOD) {
            array = bands.getCpMethodClass();
        } else if (cp == CP_IMETHOD) {
            array = bands.getCpIMethodClass();
        } else {
            throw new Error("Don't know how to handle " + cp);
        }
        realIndex = matchSpecificPoolEntryIndex(array, desiredClassName, index);
        return getConstantPoolEntry(cp, realIndex);
    }

    /**
     * Given the name of a class, answer the CPClass associated with that class.
     * Answer null if the class doesn't exist.
     *
     * @param name
     *            Class name to look for (form: java/lang/Object)
     * @return CPClass for that class name, or null if not found.
     */
    public ConstantPoolEntry getClassPoolEntry(String name) {
        String classes[] = bands.getCpClass();
        int index = matchSpecificPoolEntryIndex(classes, name, 0);
        if (index == -1) {
            return null;
        }
        try {
            return getConstantPoolEntry(CP_CLASS, index);
        } catch (Pack200Exception ex) {
            throw new Error("Error getting class pool entry");
        }
    }

    /**
     * Answer the init method for the specified class.
     *
     * @param cp
     *            constant pool to search (must be CP_METHOD)
     * @param value
     *            index of init method
     * @param desiredClassName
     *            String class name of the init method
     * @return CPMethod init method
     * @throws Pack200Exception
     */
    public ConstantPoolEntry getInitMethodPoolEntry(int cp, long value,
            String desiredClassName) throws Pack200Exception {
        int realIndex = -1;
        String desiredRegex = REGEX_MATCH_INIT;
        if (cp == CP_METHOD) {
            realIndex = matchSpecificPoolEntryIndex(bands.getCpMethodClass(),
                    bands.getCpMethodDescriptor(), desiredClassName,
                    desiredRegex, (int) value);
        } else {
            throw new Error("Nothing but CP_METHOD can be an <init>");
        }
        return getConstantPoolEntry(cp, realIndex);
    }

    /**
     * A number of things make use of subsets of structures. In one particular
     * example, _super bytecodes will use a subset of method or field classes
     * which have just those methods / fields defined in the superclass.
     * Similarly, _this bytecodes use just those methods/fields defined in this
     * class, and _init bytecodes use just those methods that start with &lt;init&gt;
     *
     * This method takes an array of names, a String to match for, an index and
     * a boolean as parameters, and answers the array position in the array of
     * the indexth element which matches (or equals) the String (depending on
     * the state of the boolean)
     *
     * In other words, if the class array consists of: Object [position 0, 0th
     * instance of Object] String [position 1, 0th instance of String] String
     * [position 2, 1st instance of String] Object [position 3, 1st instance of
     * Object] Object [position 4, 2nd instance of Object] then
     * matchSpecificPoolEntryIndex(..., "Object", 2, false) will answer 4.
     * matchSpecificPoolEntryIndex(..., "String", 0, false) will answer 1.
     *
     * @param nameArray
     *            Array of Strings against which the compareString is tested
     * @param compareString
     *            String for which to search
     * @param desiredIndex
     *            nth element with that match (counting from 0)
     * @return int index into nameArray, or -1 if not found.
     */
    protected int matchSpecificPoolEntryIndex(String[] nameArray,
            String compareString, int desiredIndex) {
        return matchSpecificPoolEntryIndex(nameArray, nameArray, compareString,
                REGEX_MATCH_ALL, desiredIndex);
    }

    /**
     * This method's function is to look through pairs of arrays. It keeps track
     * of the number of hits it finds using the following basis of comparison
     * for a hit: - the primaryArray[index] must be .equals() to the
     * primaryCompareString - the secondaryArray[index] .matches() the
     * secondaryCompareString. When the desiredIndex number of hits
     * has been reached, the index into the original two arrays of
     * the element hit is returned.
     *
     * @param primaryArray
     *            The first array to search
     * @param secondaryArray
     *            The second array (must be same .length as primaryArray)
     * @param primaryCompareString
     *            The String to compare against primaryArray using .equals()
     * @param secondaryCompareRegex
     *            The String to compare against secondaryArray using .matches()
     * @param desiredIndex
     *            The nth hit whose position we're seeking
     * @return int index that represents the position of the nth hit in
     *         primaryArray and secondaryArray
     */
    protected int matchSpecificPoolEntryIndex(String[] primaryArray,
	    String[] secondaryArray, String primaryCompareString,
	    String secondaryCompareRegex, int desiredIndex) {
	int instanceCount = -1;
	List indexList = arrayCache.indexesForArrayKey(primaryArray, primaryCompareString);
	if(indexList.isEmpty()) {
	    // Primary key not found, no chance of finding secondary
	    return -1;
	}

	for(int index=0; index < indexList.size(); index++) {
	    int arrayIndex = ((Integer)indexList.get(index)).intValue();
	    if(regexMatches(secondaryCompareRegex, secondaryArray[arrayIndex])) {
		instanceCount++;
		if(instanceCount == desiredIndex) {
		    return arrayIndex;
		}
	    }
	}
	// We didn't return in the for loop, so the desiredMatch
	// with desiredIndex must not exist in the arrays.
	return -1;
    }

    /**
     * We don't want a dependency on regex in Pack200. The only place one exists
     * is in matchSpecificPoolEntryIndex(). To eliminate this dependency, we've
     * implemented the world's stupidest regexMatch. It knows about the two
     * forms we care about: .* (aka REGEX_MATCH_ALL) ^&lt;init^gt;.* (aka
     * REGEX_MATCH_INIT) and will answer correctly if those are passed as the
     * regexString.
     *
     * @param regexString
     *            String against which the compareString will be matched
     * @param compareString
     *            String to match against the regexString
     * @return boolean true if the compareString matches the regexString;
     *         otherwise false.
     */
    protected static boolean regexMatches(String regexString,
            String compareString) {
        if (REGEX_MATCH_ALL.equals(regexString)) {
            return true;
        }
        if (REGEX_MATCH_INIT.equals(regexString)) {
            if (compareString.length() < (INITSTRING.length())) {
                return false;
            }
            return (INITSTRING.equals(compareString.substring(0, INITSTRING
                    .length())));
        }
        throw new Error("regex trying to match a pattern I don't know: "
                + regexString);
    }

    public ConstantPoolEntry getConstantPoolEntry(int cp, long value)
            throws Pack200Exception {
        int index = (int) value;
        if (index == -1) {
            return null;
        } else if (index < 0) {
            throw new Pack200Exception("Cannot have a negative range");
        } else if (cp == UTF_8) {
            return bands.cpUTF8Value(index);
        } else if (cp == CP_INT) {
            return bands.cpIntegerValue(index);
        } else if (cp == CP_FLOAT) {
            return bands.cpFloatValue(index);
        } else if (cp == CP_LONG) {
            return bands.cpLongValue(index);
        } else if (cp == CP_DOUBLE) {
            return bands.cpDoubleValue(index);
        } else if (cp == CP_STRING) {
            return bands.cpStringValue(index);
        } else if (cp == CP_CLASS) {
            return bands.cpClassValue(index);
        } else if (cp == SIGNATURE) {
            throw new Error("I don't know what to do with signatures yet");
            // return null /* new CPSignature(bands.getCpSignature()[index]) */;
        } else if (cp == CP_DESCR) {
            throw new Error("I don't know what to do with descriptors yet");
            // return null /* new CPDescriptor(bands.getCpDescriptor()[index])
            // */;
        } else if (cp == CP_FIELD) {
            return bands.cpFieldValue(index);
        } else if (cp == CP_METHOD) {
            return bands.cpMethodValue(index);
        } else if (cp == CP_IMETHOD) {
            return bands.cpIMethodValue(index);
        } else {
            // etc
            throw new Error("Get value incomplete");
        }
    }
}