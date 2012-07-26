package org.forgerock.openidm.scheduler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.yaml.snakeyaml.util.Base64Coder;

public class SimpleTests {

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        SimpleTests test = new SimpleTests();
        try {
            test.doTest();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void doTest() throws Exception {
        /*WeeklyCalendar cal = new WeeklyCalendar();
        String serializedT = serialize(cal);
        JsonValue jv = new JsonValue(serializedT);
        System.out.println(jv);
        System.out.println("isMap = " + jv.isMap());
        
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("value", "value");
        map.put("keyName", null);
        
        System.out.println(map.get("keyName"));
        */
        
        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");
        list.add("three");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("ids", list);
        JsonValue jv = new JsonValue(map);
        
        System.out.println(jv);
        
        List<String> l = (List<String>)jv.asMap().get("ids");
        
        int i=10;
        while (i>0) {
            l.add("help");
            i--;
        }
        System.out.println(jv);
    }
    
    
    /**
     * Converts a serializable object into a String.
     * 
     * @param object the object to serialize.
     * @return a string representation of the serialized object.
     * @throws Exception
     */
    private String serialize(Serializable object) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.flush();
        oos.close();
        return new String(Base64Coder.encode(baos.toByteArray()));
    }
    
    /**
     * Converts a String representation of a serialized object back
     * into an object.
     * 
     * @param str the representation of the serialized object
     * @return the deserialized object
     * @throws Exception
     */
    private Object deserialize(String str) throws Exception {
        byte [] bytes = Base64Coder.decode(str.toCharArray());
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object o  = ois.readObject();
        ois.close();
        return o;
    }
    
    private class T implements Serializable {
        private static final long serialVersionUID = 4249651332546470382L;
        public String one = "one";
        public String two = "two";
    }
    
}
