/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
 */

package org.forgerock.openidm.audit.util;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface to get log information, subject of log by and log for.
 *
 *
 * @author Laszlo Hordos
 */
public interface AuditRecord {

    /**
     * The Date/Time field. This field is one of the two required in any log
     * record.
     *
     * @supported.api
     */
    public static final String TIME = "TIME";
    /**
     * The Data field. This field is one of the two required in any log record.
     *
     * @supported.api
     */
    public static final String DATA = "Data";
    /**
     * The LogLevel field. The level at which the log record was logged.
     *
     * @supported.api
     */
    public static final String LOG_LEVEL = "LogLevel";
    /**
     * The Domain field. The AM Domain pertaining to the log record's Data
     * field.
     *
     * @supported.api
     */
    public static final String DOMAIN = "Domain";
    /**
     * The LoginID field. The AM Login ID pertaining to the log record's Data
     * field.
     *
     * @supported.api
     */
    public static final String LOGIN_ID = "LoginID";
    /**
     * The IPAddr field. The IP Address pertaining to the log record's Data
     * field.
     *
     * @supported.api
     */
    public static final String IP_ADDR = "IPAddr";
    /**
     * The HostName field. The HostName pertaining to the log record's Data
     * field.
     *
     * @supported.api
     */
    public static final String HOST_NAME = "HostName";
    /**
     * The ModuleName field. The AM Module pertaining to the log record's Data
     * field.
     *
     * @supported.api
     */
    public static final String MODULE_NAME = "ModuleName";
    /**
     * The LoggedBy field. The ID associated with who logged the record.
     *
     * @supported.api
     */
    public static final String LOGGED_BY = "LoggedBy";
    /**
     * The ContextID field. The ID associated with the user's session that is
     * the subject of this log record.
     *
     * @supported.api
     */
    public static final String CONTEXT_ID = "ContextID";
    /**
     * The MessageID field. The unique Message Identifier associated with this
     * particular log record data field.
     *
     * @supported.api
     */
    public static final String MESSAGE_ID = "MessageID";

    /**
     * The NameID field. The Name Identifier associated with this particular log
     * record data field.
     *
     * @supported.api
     */
    public static final String NAME_ID = "NameID";

    public static final String LOGIN_ID_SID = "LoginIDSid";
    public static final String LOGGED_BY_SID = "LoggedBySid";

    /**
     * Returns log information map.
     *
     * @return log information map.
     */
    Map<String, Object> getLogInfoMap();

    /**
     * Adds to the log information map, the field key and its corresponding
     * value.
     *
     * @param key
     *            The key which will be used by the formatter to determine if
     *            this piece of info is supposed to be added to the log string
     *            according to the selected log fields.
     * @param value
     *            The value which may form a part of the actual log-string.
     */
    void addLogInfo(String key, Object value);

    /**
     * Returns log by subject.
     *
     * @return log by subject.
     */
    Object getLogBy();

    /**
     * Returns log for subject.
     *
     * @return log for subject.
     */
    Object getLogFor();






    /**
     * TEMPORARY.
     */
    public class ReconEntry {

        public final static String RECON_START = "start";
        public final static String RECON_END = "summary";
        public final static String RECON_ENTRY = ""; // regular reconciliation entry has an empty entry type

        /** Type of the audit log entry. Allows for marking recon start / summary records */
        public String entryType = RECON_ENTRY;
        /** TODO: Description. */
        public final String action = null;
        public final String situation = null;
        /** The id identifying the reconciliation run */
        public String reconId;
        /** The root invocation context */
        public final Context rootContext;
        /** TODO: Description. */
        public Date timestamp;
        /** TODO: Description. */
        public Status status = Status.SUCCESS;
        /** TODO: Description. */
        public String sourceId;
        /** TODO: Description. */
        public String targetId;
        /** TODO: Description. */
        public String reconciling;
        /** TODO: Description. */
        public String message;

        private DateUtil dateUtil;

        // A comma delimited formatted representation of any ambiguous identifiers
        protected String ambigiousTargetIds;
        public void setAmbiguousTargetIds(List<String> ambiguousIds) {
            if (ambiguousIds != null) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (String id : ambiguousIds) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(id);
                }
                ambigiousTargetIds = sb.toString();
            } else {
                ambigiousTargetIds = "";
            }
        }

        private String getReconId() {
            return  reconId;
        }

        /**
         * Constructor that allows specifying the type of reconciliation log entry
         */
        private ReconEntry(Context rootContext, String entryType, DateUtil dateUtil) {
            this.rootContext = rootContext;
            this.entryType = entryType;
            this.dateUtil = dateUtil;
        }


        /**
         * TODO: Description.
         *
         * @return TODO.
         */
        private JsonValue toJsonValue() {
            JsonValue jv = new JsonValue(new HashMap<String, Object>());
            jv.put("entryType", entryType);
            jv.put("rootActionId", rootContext.getId());
            jv.put("reconId", getReconId());
            jv.put("reconciling", reconciling);
            jv.put("sourceObjectId", sourceId);
            jv.put("targetObjectId", targetId);
            jv.put("ambiguousTargetObjectIds", ambigiousTargetIds);
            jv.put("timestamp", dateUtil.formatDateTime(timestamp));
            jv.put("situation", situation);
            jv.put("action", action);
            jv.put("status", (status == null ? null : status.toString()));
            jv.put("message", message);
            return jv;
        }
    }

    class AccessLog{

        private static DateUtil dateUtil;

        /**
         * Creates a Jackson object mapper. By default, it calls
         * {@link org.codehaus.jackson.map.ObjectMapper#ObjectMapper()}.
         *
         */
        static {
            // TODO Allow for configured dateUtil
            dateUtil = DateUtil.getDateUtil("UTC");
        }

        private static AuditRecord logAuth(/*HttpServletRequest req,*/ String username, String userId,
                                    List<String> roles, String action, Status status) {

                final Map<String,Object> entry = new HashMap<String,Object>();
                entry.put("timestamp", dateUtil.now());
                entry.put("action", action.toString()); //authenticate, logout
                entry.put("status", status.toString());
                entry.put("principal", username);
                entry.put("userid", userId);
                entry.put("roles", roles);
                // check for header sent by load balancer for IPAddr of the client
                String ipAddress = null;
                /*if (logClientIPHeader == null ) {
                    ipAddress = req.getRemoteAddr();
                } else {
                    ipAddress = req.getHeader(logClientIPHeader);
                    if (ipAddress == null) {
                        ipAddress = req.getRemoteAddr();
                    }
                }*/
                entry.put("ip", ipAddress);

            return new AuditRecord() {
                @Override
                public Map<String, Object> getLogInfoMap() {
                    return entry;
                }

                @Override
                public void addLogInfo(String key, Object value) {
                    entry.put(key, value);
                }

                @Override
                public Object getLogBy() {
                    return null;
                }

                @Override
                public Object getLogFor() {
                    return null;
                }
            };
        }
    }

    class ActivityLog {

        public final static String TIMESTAMP = "timestamp";
        public final static String ACTION = "action";
        public final static String MESSAGE = "message";
        public final static String OBJECT_ID = "objectId";
        public final static String REVISION = "rev";
        public final static String ACTIVITY_ID = "activityId";
        public final static String ROOT_ACTION_ID = "rootActionId";
        public final static String PARENT_ACTION_ID = "parentActionid";
        public final static String REQUESTER = "requester";
        public final static String BEFORE = "before";
        public final static String AFTER = "after";
        public final static String STATUS = "status";
        public final static String CHANGED_FIELDS = "changedFields";
        public final static String PASSWORD_CHANGED = "passwordChanged";

        /**
         * Setup logging for the {@link ActivityLog}.
         */
        final static Logger logger = LoggerFactory.getLogger(ActivityLog.class);

        private final static boolean suspendException;
        private static DateUtil dateUtil;

        /**
         * Creates a Jackson object mapper. By default, it calls
         * {@link org.codehaus.jackson.map.ObjectMapper#ObjectMapper()}.
         *
         */
        static {
            String config =
                    IdentityServer.getInstance().getProperty(ActivityLog.class.getName().toLowerCase());
            suspendException = "suspend".equals(config);
            // TODO Allow for configured dateUtil
            dateUtil = DateUtil.getDateUtil("UTC");
        }

        private static AuditRecord buildLog(Context context, Request request, String message, String objectId, JsonValue before, JsonValue after, Status status) {
            String rev = null;
            if (after != null && after.get("_rev").isString()) {
                rev = after.get("_rev").asString();
            } else if (before != null && before.get("_rev").isString()) {
                rev = before.get("_rev").asString();
            }

            String method = request.getRequestType().name();

            // TODO: make configurable
            if (method != null && (method.equalsIgnoreCase("read") || method.equalsIgnoreCase("query"))) {
                before = null;
                after = null;
            }

            RootContext root = context.asContext(RootContext.class);
            SecurityContext security = context.asContext(SecurityContext.class);
            Context parent = context.getParent();

            //JsonValue root = JsonResourceContext.getRootContext(request);
            //JsonValue parent = JsonResourceContext.getParentContext(request);

            final Map<String, Object> activity = new HashMap<String, Object>();
            activity.put(TIMESTAMP, dateUtil.now());
            activity.put(ACTION, method);
            activity.put(MESSAGE, message);
            activity.put(OBJECT_ID, objectId);
            activity.put(REVISION, rev);
            activity.put(ACTIVITY_ID, context.getId());
            activity.put(ROOT_ACTION_ID, root.getId());
            activity.put(PARENT_ACTION_ID, parent.getId());
            activity.put(REQUESTER, security.getAuthenticationId()); //TODO Fix the authzid
            activity.put(BEFORE,  JsonUtil.jsonIsNull(before) ? null : before.getWrappedObject());
            activity.put(AFTER, JsonUtil.jsonIsNull(after) ? null : after.getWrappedObject());
            activity.put(STATUS, status == null ? null : status.toString());

            return new AuditRecord() {
                @Override
                public Map<String, Object> getLogInfoMap() {
                    return activity;
                }

                @Override
                public void addLogInfo(String key, Object value) {
                    activity.put(key, value);
                }

                @Override
                public Object getLogBy() {
                    return activity.containsKey(LOGGED_BY) ? activity.get(LOGGED_BY) : activity.get(REQUESTER);
                }

                @Override
                public Object getLogFor() {
                    return activity.get(REQUESTER);
                }
            };
        }
    }

}
