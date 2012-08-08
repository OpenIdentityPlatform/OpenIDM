package org.forgerock.openidm.scheduler;

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
