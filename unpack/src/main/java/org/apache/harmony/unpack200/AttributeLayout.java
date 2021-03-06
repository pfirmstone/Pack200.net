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

import java.util.regex.Matcher;
import org.apache.harmony.unpack200.codec.Codec;
import org.apache.harmony.unpack200.common.Pack200Exception;
import org.apache.harmony.unpack200.bytecode.ClassFileEntry;

/**
 * AttributeLayout defines a layout that describes how an attribute will be
 * transmitted.
 */
class AttributeLayout implements IMatcher {

    public static final String ACC_ABSTRACT = "ACC_ABSTRACT"; //$NON-NLS-1$
    public static final String ACC_ANNOTATION = "ACC_ANNOTATION"; //$NON-NLS-1$
    public static final String ACC_ENUM = "ACC_ENUM"; //$NON-NLS-1$
    public static final String ACC_FINAL = "ACC_FINAL"; //$NON-NLS-1$
    public static final String ACC_INTERFACE = "ACC_INTERFACE"; //$NON-NLS-1$
    public static final String ACC_NATIVE = "ACC_NATIVE"; //$NON-NLS-1$
    public static final String ACC_PRIVATE = "ACC_PRIVATE"; //$NON-NLS-1$
    public static final String ACC_PROTECTED = "ACC_PROTECTED"; //$NON-NLS-1$
    public static final String ACC_PUBLIC = "ACC_PUBLIC"; //$NON-NLS-1$
    public static final String ACC_STATIC = "ACC_STATIC"; //$NON-NLS-1$
    public static final String ACC_STRICT = "ACC_STRICT"; //$NON-NLS-1$
    public static final String ACC_SYNCHRONIZED = "ACC_SYNCHRONIZED"; //$NON-NLS-1$
    public static final String ACC_SYNTHETIC = "ACC_SYNTHETIC"; //$NON-NLS-1$
    public static final String ACC_TRANSIENT = "ACC_TRANSIENT"; //$NON-NLS-1$
    public static final String ACC_VOLATILE = "ACC_VOLATILE"; //$NON-NLS-1$
    public static final String ATTRIBUTE_ANNOTATION_DEFAULT = "AnnotationDefault"; //$NON-NLS-1$
    public static final String ATTRIBUTE_CLASS_FILE_VERSION = "class-file version"; //$NON-NLS-1$
    public static final String ATTRIBUTE_CODE = "Code"; //$NON-NLS-1$ // critical to correct interpretation of class file
    public static final String ATTRIBUTE_CONSTANT_VALUE = "ConstantValue"; //$NON-NLS-1$ // critical to correct interpretation of class file
    public static final String ATTRIBUTE_DEPRECATED = "Deprecated"; //$NON-NLS-1$
    public static final String ATTRIBUTE_ENCLOSING_METHOD = "EnclosingMethod"; //$NON-NLS-1$
    public static final String ATTRIBUTE_EXCEPTIONS = "Exceptions"; //$NON-NLS-1$
    public static final String ATTRIBUTE_INNER_CLASSES = "InnerClasses"; //$NON-NLS-1$
    public static final String ATTRIBUTE_LINE_NUMBER_TABLE = "LineNumberTable"; //$NON-NLS-1$
    public static final String ATTRIBUTE_LOCAL_VARIABLE_TABLE = "LocalVariableTable"; //$NON-NLS-1$
    public static final String ATTRIBUTE_LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable"; //$NON-NLS-1$
    public static final String ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations"; //$NON-NLS-1$
    public static final String ATTRIBUTE_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations"; //$NON-NLS-1$
    public static final String ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations"; //$NON-NLS-1$
    public static final String ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations"; //$NON-NLS-1$
    public static final String ATTRIBUTE_SIGNATURE = "Signature"; //$NON-NLS-1$
    public static final String ATTRIBUTE_SOURCE_FILE = "SourceFile"; //$NON-NLS-1$
    //Java 6 Attributes
    public static final String ATTRIBUTE_STACK_MAP_TABLE = "StackMapTable"; // critical to correct interpretation of class file
    //Java 7 Attributes
    public static final String ATTRIBUTE_BOOTSTRAP_METHODS = "BootstrapMethods"; // critical to correct interpretation of class file
    //Java 8 Attributes
    public static final String ATTRIBUTE_RUNTIME_VISIBLE_TYPE_ANNOTATIONS = "RuntimeVisibleTypeAnnotations";
    public static final String ATTRIBUTE_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = "RuntimeInvisibleTypeAnnotations";
    public static final String ATTRIBUTE_METHOD_PARAMETERS = "MethodParameters";
    //Java 9 Attributes
    public static final String ATTRIBUTE_MODULE = "Module";
    public static final String ATTRIBUTE_MODULE_PACKAGES = "ModulePackages";
    public static final String ATTRIBUTE_MODULE_MAIN_CLASS = "ModuleMainClass";
    
    public static final int CONTEXT_CLASS = 0;
    public static final int CONTEXT_CODE = 3;
    public static final int CONTEXT_FIELD = 1;
    public static final int CONTEXT_METHOD = 2;
    public static final String[] contextNames = { "Class", "Field", "Method", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Code", }; //$NON-NLS-1$
    
    private static ClassFileEntry getValue(String layout, long value,
            SegmentConstantPool pool) throws Pack200Exception {
        if (layout.startsWith("R")) { //$NON-NLS-1$
            // references
            if (layout.indexOf('N') != -1) value--; // contains N
	    char type = layout.charAt(1);
	    switch (type) {
		case 'U': //$NON-NLS-1$
		    return pool.getValue(SegmentConstantPool.UTF_8, value);
		case 'S': //$NON-NLS-1$
		    return pool.getValue(SegmentConstantPool.SIGNATURE, value);
		case 'C':
		    return pool.getValue(SegmentConstantPool.CP_CLASS, value);
		case 'D':
		    return pool.getValue(SegmentConstantPool.CP_DESCR, value);
		case 'F':
		    return pool.getValue(SegmentConstantPool.CP_FIELD, value);
		case 'M':
		    return pool.getValue(SegmentConstantPool.CP_METHOD, value);
		case 'I':
		    return pool.getValue(SegmentConstantPool.CP_IMETHOD, value);
		case 'Y':
		    return pool.getValue(SegmentConstantPool.CP_INVOKE_DYNAMIC, value);
		case 'B':
		    return pool.getValue(SegmentConstantPool.CP_BOOTSTRAP_METHOD, value);
		case 'Q':
		    return pool.getValue(SegmentConstantPool.ALL, value);
		default:
		    throw new Pack200Exception("Unknown layout encoding: " + layout);
	    }
        } else if (layout.startsWith("K")) { //$NON-NLS-1$
            char type = layout.charAt(1);
            switch (type) {
		case 'S': // String
		    return pool.getValue(SegmentConstantPool.CP_STRING, value);
		case 'I': // Int (or byte or short)
		case 'C': // Char
		    return pool.getValue(SegmentConstantPool.CP_INT, value);
		case 'F': // Float
		    return pool.getValue(SegmentConstantPool.CP_FLOAT, value);
		case 'J': // Long
		    return pool.getValue(SegmentConstantPool.CP_LONG, value);
		case 'D': // Double
		    return pool.getValue(SegmentConstantPool.CP_DOUBLE, value);
		case 'M': // MethodHandle
		    return pool.getValue(SegmentConstantPool.CP_METHOD_HANDLE, value);
		case 'T': // MethodType
		    return pool.getValue(SegmentConstantPool.CP_METHOD_TYPE, value);
		case 'L': // Loadable value group
		    return pool.getValue(SegmentConstantPool.CP_LOADABLE_VALUE, value);
		default:
		    throw new Pack200Exception("Unknown layout encoding: " + layout);
            }
        }
        throw new Pack200Exception("Unknown layout encoding: " + layout);
    }

    private final int context;

    private final int index;

    private final String layout;

    private long mask;

    private final String name;
    private final boolean isDefault;
    private int backwardsCallCount;

    /**
     * Construct a default AttributeLayout (equivalent to
     * <code>new AttributeLayout(name, context, layout, index, true);</code>)
     *
     * @param name
     * @param context
     * @param layout
     * @param index
     * @throws Pack200Exception
     */
    public AttributeLayout(String name, int context, String layout, int index)
            throws Pack200Exception {
        this(name, context, layout, index, true);
    }

    public AttributeLayout(String name, int context, String layout, int index,
            boolean isDefault) throws Pack200Exception {
	super();
	this.index = index;
	this.context = context;
	if (index >= 0) {
	    this.mask = 1L << index;
	} else {
	    this.mask = 0;
	}
	if (context != CONTEXT_CLASS && context != CONTEXT_CODE && context != CONTEXT_FIELD && context != CONTEXT_METHOD) {
	    throw new Pack200Exception("Attribute context out of range: " + context);
	}
	if (layout == null) {
	    throw new Pack200Exception("Cannot have a null layout");
	}
	if (name == null || name.length() == 0) {
	    throw new Pack200Exception("Cannot have an unnamed layout");
	}
	this.name = name;
	this.layout = layout;
	this.isDefault = isDefault;
	char [] lay = layout.toCharArray();
	int backCallCount = 0;
	for (int i = 0, l = lay.length; i < l; i++){
	    if (i > 0 && 
		(lay[i] == 48 /*0*/ || 
		(lay[i-1] == '-' && lay[i] >= 48 /*0*/ && lay[i] <= 57 /*9*/)))
	    {
		for ( int b = i - 1; b > 0; b--){
		    if (lay[b] == 93 /*]*/ || lay[b] <=90 /*Z*/ && lay[b] >= 65 /*A*/)  break;
		    if (lay[b] == 91 /*[*/){
			backCallCount ++;
			break;
		    }
		}
	    }
	}
	this.backwardsCallCount = backCallCount;
    }

    public Codec getCodec() {
        if (layout.indexOf('O') >= 0) {
            return Codec.BRANCH5;
        } else if (layout.indexOf('P') >= 0) {
            return Codec.BCI5;
        } else if (layout.indexOf('S') >= 0 && layout.indexOf("KS") < 0 //$NON-NLS-1$
                && layout.indexOf("RS") < 0) { //$NON-NLS-1$
            return Codec.SIGNED5;
        } else if (layout.indexOf('B') >= 0 && layout.indexOf("RB") < 0) { //RB governs indexes to bootstrap method specifiers
            return Codec.BYTE1;
        } else {
            return Codec.UNSIGNED5;
        }
    }

    public String getLayout() {
        return layout;
    }

    public ClassFileEntry getValue(long value, SegmentConstantPool pool)
            throws Pack200Exception {
        return getValue(layout, value, pool);
    }

    public ClassFileEntry getValue(long value, String type, SegmentConstantPool pool)
            throws Pack200Exception {
        // TODO This really needs to be better tested, esp. the different types
        // TODO This should have the ability to deal with RUN stuff too, and
        // unions
        if (layout.startsWith("KQ")) { //$NON-NLS-1$
            if (type.equals("Ljava/lang/String;")) { //$NON-NLS-1$
                ClassFileEntry value2 = getValue("KS", value, pool); //$NON-NLS-1$
                return value2;
            } else {
                return getValue("K" + type + layout.substring(2), value, //$NON-NLS-1$
                        pool);
            }
        } else {
            return getValue(layout, value, pool);
        }
    }

    public int hashCode() {
        int PRIME = 31;
        int r = 1;
        if (name != null) {
            r = r * PRIME + name.hashCode();
        }
        if (layout != null) {
            r = r * PRIME + layout.hashCode();
        }
        r = r * PRIME + index;
        r = r * PRIME + context;
        return r;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.harmony.unpack200.IMatches#matches(long)
     */
    public boolean matches(long value) {
        return (value & mask) != 0;
    }

    public String toString() {
        return contextNames[context] + ": " + name;
    }

    public int getContext() {
        return context;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public int numBackwardsCallables() {
	return backwardsCallCount;
    }

    public boolean isDefaultLayout() {
        return isDefault;
    }

    public void setBackwardsCallCount(int backwardsCallCount) {
        this.backwardsCallCount = backwardsCallCount;
    }

}
