/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.context;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InvokeContext {
    private final static Logger logger = LoggerFactory.getLogger(InvokeContext.class);
    
    private static ThreadLocalContext ctx = new ThreadLocalContext();
    
    public static ContextMap getContext() {
        return ctx.getContextMap();
    }

    @SuppressWarnings("rawtypes")
    private static class ThreadLocalContext extends ThreadLocal {
        public Object initialValue() {
          return new ContextMapImpl();
        }
    
        @SuppressWarnings("unchecked")
        public ContextMap getContextMap() { 
          return (ContextMap) super.get(); 
        }
    }
    
    private static class ContextMapImpl extends HashMap<String, Object> implements ContextMap {
        private final static Logger logger = LoggerFactory.getLogger(ContextMapImpl.class);
        
        static final long serialVersionId = 1L;
        
        public void pushNestedEntry(String key, Object additionalEntry) {
            Object nestedEntry = get(key);
            if (nestedEntry == null) {
                nestedEntry = new LinkedList();
                put(key, nestedEntry);
            } else if (!(nestedEntry instanceof LinkedList)) {
                throw new IllegalArgumentException("Entry for key " + key + " is not of type LinkedList but of type " 
                        + nestedEntry.getClass() + ", and does not support pushing nested entries.");
            }
            ((LinkedList) nestedEntry).addLast(additionalEntry);
        }
        
        public Object popNestedEntry(String key) {
            Object nestedEntry = get(key);
            Object removed = null;
            if (nestedEntry != null && !(nestedEntry instanceof LinkedList)) {
                throw new IllegalArgumentException("Entry for key " + key + " is not of type LinkedList but of type " 
                        + nestedEntry.getClass() + ", and does not support removing nested entries.");
            } else if (nestedEntry == null) {
                throw new IllegalArgumentException("Entry for key " + key + " does not exist. " 
                        + " Can not pop off the nested entry if it didn't get pushed");
            } else if (((List)nestedEntry).size() == 0) {
                logger.warn("Requested to pop entry off of " + key + " but no entries are in the list. " 
                        + "this could point to a mismatched push/pop.");
            } else {
                removed = ((LinkedList) nestedEntry).removeLast();
            }
            return removed;
        }
        
        public List<Object> getNestedEntries(String key) {
            return (List) get(key);
        }
        
        public Object getFirstNested(String key) {
            return ((LinkedList) get(key)).getFirst();
        }
        
        public Object getLastNested(String key) {
            return ((LinkedList) get(key)).getLast();
        }
        
        public void pushActivityId(String activityId) {
            pushNestedEntry(NESTED_ACTIVITY_IDS, activityId);
        }
        
        public String popActivityId() {
            return (String) popNestedEntry(NESTED_ACTIVITY_IDS);
        }
        
        public String getRequester() {
            return (String) get(REQUESTER);
        }
        
        public void putRequester(String requester) {
            put(REQUESTER, requester);
        }
        
        public void removeRequester() {
            remove(REQUESTER);
        }
        
        public String getApprover() {
            return (String) get(APPROVER);
        }
        
        public void putApprover(String approver) {
            put(APPROVER, approver);
        }
        
        public void removeApprover() {
            remove(APPROVER);
        }
    }
}