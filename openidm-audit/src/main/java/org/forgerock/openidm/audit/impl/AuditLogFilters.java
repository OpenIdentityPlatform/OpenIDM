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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

import static org.forgerock.openidm.audit.impl.AuditServiceImpl.TYPE_ACTIVITY;
import static org.forgerock.openidm.audit.impl.AuditServiceImpl.TYPE_RECON;


/**
 * A utility class containing various factory methods for creating {@link AuditLogFilter}s.
 *
 * @author brmiller
 */
public class AuditLogFilters {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    /**
     * A NullObject implementation that never filters.
     */
    public static AuditLogFilter NONE = new AuditLogFilter() {
        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            return false;
        }
    };

    /**
     * A filter that filters out any actions <em>A</em> that are not contained
     * in the set of {em}actionsToLog{em}
     *
     * @param <A> the action enum type
     */
    private static class ActionFilter<A extends Enum<A>> implements AuditLogFilter {

        private final Class<A> clazz;
        private final Set<A> actionsToLog;

        private ActionFilter(Class<A> clazz, Set<A> actionsToLog) {
            this.clazz = clazz;
            this.actionsToLog = actionsToLog;
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            JsonValue action = request.getContent().get("action");
            // don't filter requests that do not specify an action
            if (action.isNull()) {
                return false;
            }
            try {
                return !actionsToLog.contains(action.asEnum(clazz));
            } catch (IllegalArgumentException e) {
                // don't filter an action that isn't one of the designated enum constants
                return false;
            }
        }
    }

    /**
     * A filter that filters on <em>eventType</em> and another filter.
     */
    private static class EventTypeFilter implements AuditLogFilter {

        private final String eventType;
        private final AuditLogFilter filter;

        private EventTypeFilter(String eventType, AuditLogFilter filter) {
            this.eventType = eventType;
            this.filter = filter;
        }

        private String getEventType(CreateRequest request) {
            return request.getResourceNameObject().head(1).toString();
        }

        private boolean isEventType(CreateRequest request) {
            return eventType.equals(getEventType(request));
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            return isEventType(request) && filter.isFiltered(context, request);
        }
    }

    /**
     * A filter that filters on a TriggerContext trigger and another filter.
     */
    private static class TriggerFilter implements AuditLogFilter {

        private final String trigger;
        private final AuditLogFilter filter;

        private TriggerFilter(String trigger, AuditLogFilter filter) {
            this.trigger = trigger;
            this.filter = filter;
        }

        /**
         * Searches the context chain for a TriggerContext and returns the trigger value if found.
         *
         * @param context the context chain
         * @return the trigger value if the chain contains a trigger context, null if it does not
         */
        private String getTrigger(Context context) {
            /*
            String trigger = null;
            // Loop through parent contexts, and return highest "trigger"
            while (context != null) {
                JsonValue tmp = (JsonValue) context.getParams().get("trigger");
                if (!tmp.isNull()) {
                    trigger = tmp.asString();
                }
                context = context.getParent();
            }
            */
            return context.containsContext(TriggerContext.class)
                    ? context.asContext(TriggerContext.class).getTrigger()
                    : null;
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            return trigger.equals(getTrigger(context)) && filter.isFiltered(context, request);
        }
    }

    /**
     * A filter implemented via a {@link ScriptEntry}.
     */
    private static class ScriptedFilter implements AuditLogFilter {
        private ScriptEntry scriptEntry;

        private ScriptedFilter(ScriptEntry scriptEntry) {
            this.scriptEntry = scriptEntry;
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            if (!scriptEntry.isActive()) {
                // do not filter if script has become in active
                return false;
            }

            Script script = scriptEntry.getScript(context);
            script.put("request", request);
            script.put("context", context);
            try {
                // Flip the polarity of the script return.  We want the customer-facing semantic to be filter-in,
                // but the implementation uses a filter-out paradigm.
                return !((Boolean) script.eval());
            } catch (ScriptException e) {
                logger.warn("Audit filter script {} threw exception {} - not filtering", scriptEntry.getName().getName(), e.toString());
                return false;
            }
        }
    }

    /**
     * A composite filter that filters if any one of the constituents filters.
     */
    private static class CompositeFilter implements AuditLogFilter {

        private final List<AuditLogFilter> filters;

        private CompositeFilter(List<AuditLogFilter> filters) {
            this.filters = new ArrayList<AuditLogFilter>();
            // only add filters which are not the NONE filter
            for (AuditLogFilter filter : filters) {
                if (!filter.equals(NONE)) {
                    this.filters.add(filter);
                }
            }
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            for (AuditLogFilter filter : filters) {
                if (filter.isFiltered(context, request)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static <A extends Enum<A>> Set<A> getActions(Class<A> actionClass, JsonValue actions) {
        final Set<A> filter = EnumSet.noneOf(actionClass);

        // Refactor to asSet(new Function() -> { return asEnum(actionClass); })
        // after CREST-169
        for (JsonValue action : actions) {
            try {
                filter.add(action.asEnum(actionClass));
            } catch (IllegalArgumentException e) {
                logger.warn("Action value {} is not a known filter action", new Object[] { action.toString() });
            }
        }
        return filter;
    }

    /**
     * Creates a new action-filter for the <em>activity</em> event type.  If a request action is null or a value
     * that is not a {@link RequestType} constant, it will not be filtered.  It is also not possible to specify
     * filtering of null, or non-{@link RequestType} values.
     *
     * @param actions a JsonValue list of action values
     * @return an AuditLogFilter that filters activity actions.
     */
    static AuditLogFilter newActivityActionFilter(JsonValue actions) {
        return new EventTypeFilter(TYPE_ACTIVITY,
                new ActionFilter<RequestType>(RequestType.class, getActions(RequestType.class, actions)));
    }

    /**
     * Creates a new trigger-action-filter for the <em>activity</em> event type.  If a request action is null or a
     * value that is not a {@link RequestType} constant, it will not be filtered.  It is also not possible to specify
     * filtering of null, or non-{@link RequestType} values.
     *
     * @param actions a JsonValue list of action values
     * @param trigger a trigger to filter on
     * @return an AuditLogFilter that filters activity actions for a particular trigger.
     */
    static AuditLogFilter newActivityActionFilter(JsonValue actions, String trigger) {
        return new EventTypeFilter(TYPE_ACTIVITY,
                new TriggerFilter(trigger,
                        new ActionFilter<RequestType>(RequestType.class, getActions(RequestType.class, actions))));
    }

    /**
     * Creates a new action-filter for the <em>recon</em> event type.  If a request action is null or a value
     * that is not a {@link ReconAction} constant, it will not be filtered.  It is also not possible to specify
     * filtering of null, or non-{@link ReconAction} values.
     *
     * @param actions a JsonValue list of action values
     * @return an AuditLogFilter that filters recon actions.
     */
    static AuditLogFilter newReconActionFilter(JsonValue actions) {
        return new EventTypeFilter(TYPE_RECON,
                new ActionFilter<ReconAction>(ReconAction.class, getActions(ReconAction.class, actions)));
    }

    /**
     * Creates a new trigger-action-filter for the <em>recon</em> event type.  If a request action is null or a
     * value that is not a {@link ReconAction} constant, it will not be filtered.  It is also not possible to specify
     * filtering of null, or non-{@link ReconAction} values.
     *
     * @param actions a JsonValue list of action values
     * @param trigger a trigger to filter on
     * @return an AuditLogFilter that filters recon actions for a particular trigger.
     */
    static AuditLogFilter newReconActionFilter(JsonValue actions, String trigger) {
        return new EventTypeFilter(TYPE_RECON,
                new TriggerFilter(trigger,
                        new ActionFilter<ReconAction>(ReconAction.class, getActions(ReconAction.class, actions))));
    }

    /**
     * Creates a composite audit log filter that filters if any of its constituent filters filters.
     * Simplifies to a {@link #NONE} "never-filter" if the filter list is empty or only contains {@link #NONE} filters.
     *
     * @param filters the list of audit log filters
     * @return a composite filter of filters, or NONE, if there are no filters
     */
    static AuditLogFilter newCompositeActionFilter(List<AuditLogFilter> filters) {
        return filters.isEmpty() || onlyContainsNone(filters)
                ? NONE // don't bother creating a composite filter out of nothing or a bunch of nothings
                : new CompositeFilter(filters);
    }

    /**
     * Creates an audit log filter implemented in a script.
     *
     * @param scriptEntry the Script
     * @return an audit log filter via script
     */
    static AuditLogFilter newScriptedFilter(ScriptEntry scriptEntry) {
        return new ScriptedFilter(scriptEntry);
    }

    /**
     * Returns true if the collection of filters only contains {@link #NONE} filters, false if it contains at least one
     * non-{@link #NONE} filter
     *
     * @param collection the collection of filters to test
     * @return true if the collection of filters only contains {@link #NONE} filters, false if it contains at least one
     *         non-{@link #NONE} filter
     */
    private static final boolean onlyContainsNone(Collection<AuditLogFilter> collection) {
        for (AuditLogFilter element : collection) {
            if (!element.equals(NONE)) {
                return false;
            }
        }
        return true;
    }
}
