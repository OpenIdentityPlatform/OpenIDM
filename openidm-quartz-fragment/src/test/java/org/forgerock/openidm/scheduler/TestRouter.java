package org.forgerock.openidm.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;

public class TestRouter implements ObjectSet {
    
    private Map<String, Object> map;
    private boolean printMap = false;
    
    public TestRouter() {
        map = new HashMap<String, Object>();
    }

    @Override
    public void create(String id, Map<String, Object> object)
            throws ObjectSetException {
        printMap("create",object);
        map.put(id, object);

    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException, NotFoundException {
        // TODO Auto-generated method stub
        Map<String, Object> object = (Map<String, Object>)map.get(id);
        printMap("read", object);
        if(object == null) {
            throw new NotFoundException("");
        }
        return object;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object)
            throws ObjectSetException {
        printMap("update", object);
        map.put(id, object);
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        map.remove(id);
    }

    @Override
    public void patch(String id, String rev, Patch patch)
            throws ObjectSetException {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params)
            throws ObjectSetException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params)
            throws ObjectSetException {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void printMap(String method, Map<String, Object> map) {
        if (printMap) {
            if (map != null) {
                Set<String> keys = map.keySet();
                for (String key : keys) {
                    System.out.println(method + " [" + key + ", "
                            + map.get(key) + "]");
                }
            } else {
                System.out.println(method + " null");
            }
        }
    }

}
