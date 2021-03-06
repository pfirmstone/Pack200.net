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
package org.apache.harmony.pack200;

/**
 * Constant pool entry for an int.
 */
class CPInt extends CPConstant {

    private final int theInt;

    public CPInt(int theInt) {
        this.theInt = theInt;
    }

    public int compareTo(Object obj) {
        if(theInt > ((CPInt)obj).theInt) {
            return 1;
        } else if (theInt == ((CPInt)obj).theInt) {
            return 0;
        } else {
            return -1;
        }
    }
    
    @Override
    public boolean equals(Object o){
	if (!(o instanceof CPInt)) return false;
	return theInt == ((CPInt)o).theInt;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 29 * hash + this.theInt;
	return hash;
    }

    public int getInt() {
        return theInt;
    }
}
