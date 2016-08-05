/*
 * Copyright (c) 2009-2012 Panxiaobo
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
package com.googlecode.d2j;

import java.util.Objects;

/**
 * represent a field_id_item in dex file format
 * 
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 * @version $Rev$
 */
public class Field {
    /**
     * name of the field.
     */
    private final String name;
    /**
     * owner class of the field, in TypeDescriptor format.
     */
    private final String owner;
    /**
     * type of the field, in TypeDescriptor format.
     */
    private final String type;

    private transient int hash;

    public Field(String owner, String name, String type) {
        this.owner = owner;
        this.type = type;
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return owner + "." + name + " " + type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Field) {
            Field o = (Field) obj;
            return Objects.equals(owner, o.owner)
                    && Objects.equals(name, o.name)
                    && Objects.equals(type, o.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h != 0) {
            return h;
        }
        h = name != null ? name.hashCode() : 0;
        h = 31 * h + (owner != null ? owner.hashCode() : 0);
        h = 31 * h + (type != null ? type.hashCode() : 0);
        return hash = h;
    }
}
