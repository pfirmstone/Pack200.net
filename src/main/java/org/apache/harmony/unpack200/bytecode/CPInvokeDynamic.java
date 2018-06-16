/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.harmony.unpack200.bytecode;

import java.util.List;

/**
 * Alpha level code, superclass and methods may change.
 * @author peter
 */
public class CPInvokeDynamic extends CPMember {

    public CPInvokeDynamic(CPUTF8 name, CPUTF8 descriptor, long flags, List attributes) {
	super(name, descriptor, flags, attributes);
    }
    
    @Override
    public String toString() {
        return "InvokeDynamic: " + name + "(" + descriptor + ")";
    }
    
}
