/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://forgerock.org/license/CDDLv1.0.html
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at http://forgerock.org/license/CDDLv1.0.html
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
*/

package org.forgerock.openidm.quartz.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;
import org.quartz.JobPersistenceException;

public class RepoJobStoreUtils {

    /**
     * Converts a serializable object into a String.
     * 
     * @param object the object to serialize.
     * @return a string representation of the serialized object.
     * @throws Exception
     */
    public static String serialize(Serializable object) throws JobPersistenceException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            oos.close();
            //return new String(Base64Coder.encode(baos.toByteArray()));
            return new String(Base64.encodeBase64(baos.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new JobPersistenceException(e.getMessage());
        }
    }
    
    /**
     * Converts a String representation of a serialized object back
     * into an object.
     * 
     * @param str the representation of the serialized object
     * @return the deserialized object
     * @throws Exception
     */
    public static Object deserialize(String str) throws JobPersistenceException {
        try {
            //byte [] bytes = Base64Coder.decode(str.toCharArray());
            byte [] bytes = Base64.decodeBase64(str);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object o  = ois.readObject();
            ois.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            throw new JobPersistenceException(e.getMessage());
        }
    }
}
