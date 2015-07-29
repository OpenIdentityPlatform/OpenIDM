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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.util.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;


/**
 * A utility class containing various factory methods for creating {@link AuditLogFilter}s.
 *
 */
public class AuditLogFilters {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    private static final String TYPE_ACTIVITY = "activity";
    private static final String TYPE_RECON = "recon";

    /** Type alias for converting the value of a JsonValue to a particular type */
    interface JsonValueObjectConverter<V> extends Function<JsonValue, V, JsonValueException> {}

    /**
     * A NullObject implementation that never filters.
     */
    public static AuditLogFilter NEVER = new AuditLogFilter() {
        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            return false;
        }
    };

    /** JsonValueObjectConverter for returning the object as a String */
    static final JsonValueObjectConverter<String> AS_STRING =
            new JsonValueObjectConverter<String>() {
                @Override
                public String apply(JsonValue value) {
                    return value != null ? value.asString() :  null;
                }
            };

    /** JsonValueObjectConverter for returning the object a named field-values filter */
    static final JsonValueObjectConverter<AuditLogFilter> AS_SINGLE_FIELD_VALUES_FILTER =
            new JsonValueObjectConverter<AuditLogFilter>() {
                @Override
                public AuditLogFilter apply(JsonValue value) {
                    return newFieldValueFilter(
                            new JsonPointer(value.get("name").required().asString()),
                            value.get("values").required().asSet(String.class),
                            AS_STRING); // currently assumes values are strings
                }
            };

    /**
     * A filter that filters on a set of fields and their values.  The log entry is
     * filtered out if the field value in the request is not in the values-set.
     */
    private static class FieldValueFilter<V> implements AuditLogFilter {
        JsonPointer field;
        Set<V> values;
        JsonValueObjectConverter<V> asValue;

        FieldValueFilter(JsonPointer field, Set<V> values, JsonValueObjectConverter<V> asValue) {
            this.field = field;
            this.values = values;
            this.asValue = asValue;
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            return !values.contains(asValue.apply(request.getContent().get(field)));
        }
    }

    /**
     * A filter that filters out any actions <em>A</em> that are not contained
     * in the set of {em}actionsToLog{em}
     *
     * @param <A> the action enum type
     */
    private static class ActionFilter<A extends Enum<A>> extends FieldValueFilter<A> {

        private static final JsonPointer FIELD_ACTION = new JsonPointer("action");

        private ActionFilter(final Class<A> clazz, final Set<A> actionsToLog) {
            super(FIELD_ACTION, actionsToLog, new JsonValueObjectConverter<A>() {
                public A apply(JsonValue value) throws JsonValueException {
                    return value.asEnum(clazz);
                }
            });
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            // don't filter requests that do not specify an action
            if (request.getContent().get(field).isNull()) {
                return false;
            }
            try {
                return super.isFiltered(context, request);
            } catch (IllegalArgumentException e) {
                // don't filter an action that isn't one of the designated enum constants
                return false;
            }
        }
    }

    /**
     * A filter that filters out any CRUDPAQ method <em>A</em> that are not contained
     * in the set of {em}actionsToLog{em}
     *
     * @param <A> the action enum type
     */
    private static class ResourceOperationFilter<A extends Enum<A>> extends FieldValueFilter<A> {

        private static final JsonPointer RESOURCE_OPERATION_METHOD =
                new JsonPointer("resourceOperation/operation/method");

        private ResourceOperationFilter(final Class<A> clazz, final Set<A> actionsToLog) {
            super(RESOURCE_OPERATION_METHOD, actionsToLog, new JsonValueObjectConverter<A>() {
                public A apply(JsonValue value) throws JsonValueException {
                    return value.asEnum(clazz);
                }
            });
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            // don't filter requests that do not specify an action
            if (request.getContent().get(field).isNull()) {
                return false;
            }
            try {
                return super.isFiltered(context, request);
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
                // do not filter if script has become inactive
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
     * An abstract composite filter that wraps a list of other filters.
     */
    private abstract static class CompositeFilter implements AuditLogFilter {

        final List<AuditLogFilter> filters;

        private CompositeFilter(List<AuditLogFilter> filters) {
            this.filters = new ArrayList<>();
            for (AuditLogFilter filter : filters) {
                if (!filter.equals(NEVER)) {
                    this.filters.add(filter);
                }
            }
        }

        @Override
        public abstract boolean isFiltered(ServerContext context, CreateRequest request);
    }

    /**
     * A composite filter that filters if any one of the component filters.
     * <p>
     * Returns an {@code AuditLogFilter} whose {@code isFiltered} returns true if any one of its
     * components' {@code isFiltered} returns true. The components are evaluated in order, and
     * evaluation will be "short-circuited" as soon as a component filter whose {@code isFiltered}
     * returns true is found.
     */
    private static class OrCompositeFilter extends CompositeFilter {

        private OrCompositeFilter(List<AuditLogFilter> filters) {
            super(filters);
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

    /**
     * A composite filter that only filters if all of the component filters.
     * <p>
     * Returns an {@code AuditLogFilter} whose {@code isFiltered} returns true if each of its
     * components' {@code isFiltered} returns true. The components are evaluated in order, and
     * evaluation will be "short-circuited" as soon as a filter whose {@code isFiltered} returns
     * false is found.
     */
    private static class AndCompositeFilter extends CompositeFilter {

        private AndCompositeFilter(List<AuditLogFilter> filters) {
            super(filters);
        }

        @Override
        public boolean isFiltered(ServerContext context, CreateRequest request) {
            for (AuditLogFilter filter : filters) {
                if (!filter.isFiltered(context, request)) {
                    // short-circuit on a single filter that does not filter
                    return false;
                }
            }
            return true;
        }
    }

    private static <A extends Enum<A>> Set<A> getActions(Class<A> actionClass, JsonValue actions) {
        final Set<A> filter = EnumSet.noneOf(actionClass);

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
     * Creates an audit filter on a particular event type.
     *
     * @param eventType the eventType to consider on this filter
     * @param filter the filter to apply
     * @return an AuditLogFilter that applies the given filter if the record's eventType matches
     */
    static AuditLogFilter newEventTypeFilter(String eventType, AuditLogFilter filter) {
        return new EventTypeFilter(eventType, filter);
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
        return newEventTypeFilter(TYPE_ACTIVITY,
                new ResourceOperationFilter<>(RequestType.class, getActions(RequestType.class, actions)));
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
        return newEventTypeFilter(TYPE_ACTIVITY,
                new TriggerFilter(trigger,
                        new ResourceOperationFilter<>(RequestType.class, getActions(RequestType.class, actions))));
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
        return newEventTypeFilter(TYPE_RECON,
                new ActionFilter<>(ReconAction.class, getActions(ReconAction.class, actions)));
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
        return newEventTypeFilter(TYPE_RECON,
                new TriggerFilter(trigger,
                        new ActionFilter<>(ReconAction.class, getActions(ReconAction.class, actions))));
    }

    /**
     * Creates a composite audit log filter that filters if any of its component filters filter.
     * <p>
     * Returns an {@code AuditLogFilter} whose {@code isFiltered} returns true if any one of its
     * components' {@code isFiltered} returns true. The component filters are evaluated in order, and
     * evaluation will be "short-circuited" as soon as a filter whose {@code isFiltered} returns true
     * is found.
     * <p>
     * Simplifies to a {@link #NEVER} "never-filter" if the filter list is empty or only contains
     * {@link #NEVER} filters.
     *
     * @param filters the list of audit log filters
     * @return a composite filter of filters that filters when a single filter in the list filters,
     *         or NEVER, if there are no filters
     */
    static AuditLogFilter newOrCompositeFilter(List<AuditLogFilter> filters) {
        return filters.isEmpty() || onlyContainsNever(filters)
                ? NEVER // don't bother creating a composite filter out of only never-filters
                : new OrCompositeFilter(filters);
    }

    /**
     * Creates a composite audit log filter that filters if all of its component filters filter.
     * <p>
     * Returns an {@code AuditLogFilter} whose {@code isFiltered} returns true if each of its
     * components' {@code isFiltered} returns true. The components are evaluated in order, and
     * evaluation will be "short-circuited" as soon as a filter whose {@code isFiltered} returns
     * false is found.
     * <p>
     * Simplifies to a {@link #NEVER} "never-filter" if the filter list is empty or only contains
     * {@link #NEVER} filters.
     *
     * @param filters the list of fields and values
     * @return a composite audit log filter that filters only when ALL filters filter,
     *         or NEVER, if there are no filters
     */
    static AuditLogFilter newAndCompositeFilter(List<AuditLogFilter> filters) {
        return filters.isEmpty() || onlyContainsNever(filters)
                ? NEVER // don't bother creating a composite filter out of only never-filters
                : new AndCompositeFilter(filters);
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
     * Creates an audit log filter implemented in a script for a particular event type.
     *
     * @param eventType the event type
     * @param scriptEntry the Script
     * @return an audit log filter via script
     */
    static AuditLogFilter newScriptedFilter(String eventType, ScriptEntry scriptEntry) {
        return newEventTypeFilter(eventType, newScriptedFilter(scriptEntry));
    }

    /**
     * Creates an field-value filter.
     *
     * @param <V> the value type
     * @param field the field to filter on
     * @param values the set of values for {@code field} that, if present in the request body, will
     *               avoid being filtered out
     * @param asValue a transformation function for the type of field values
     * @return an audit log filter that includes (does not filter) records whose value for a particular
     *         field is in a set of values
     */
    static <V> AuditLogFilter newFieldValueFilter(JsonPointer field, Set<V> values,
            final JsonValueObjectConverter<V> asValue) {
        return new FieldValueFilter<>(field, values, asValue);
    }

    /**
     * Returns true if the collection of filters only contains {@link #NEVER} filters, false if it contains at least one
     * non-{@link #NEVER} filter
     *
     * @param collection the collection of filters to test
     * @return true if the collection of filters only contains {@link #NEVER} filters, false if it contains at least one
     *         non-{@link #NEVER} filter
     */
    private static final boolean onlyContainsNever(Collection<AuditLogFilter> collection) {
        for (AuditLogFilter element : collection) {
            if (!element.equals(NEVER)) {
                return false;
            }
        }
        return true;
    }
}
