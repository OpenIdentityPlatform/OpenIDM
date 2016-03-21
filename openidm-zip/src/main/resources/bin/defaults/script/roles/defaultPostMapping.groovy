/**
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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package bin.defaults.script.roles

import static org.forgerock.json.JsonValue.object
import static org.forgerock.json.JsonValue.field
import static org.forgerock.json.resource.ResourcePath.resourcePath

import org.forgerock.openidm.util.DateUtil
import org.forgerock.json.JsonValue
import org.forgerock.openidm.sync.SyncContext

def syncContext = context.containsContext(SyncContext.class) \
    ? context.asContext(SyncContext.class)
    : null;

def mappingSource = mappingConfig.source.getObject() as String
def sourceObject = source as JsonValue
def oldSource = oldSource as JsonValue

mappingName =  mappingConfig.name.getObject() as String

try {
    if (!mappingSource.equals("managed/user") && syncContext == null) {
        return;
    }

    JsonValue lastSyncEffectiveAssignments = oldSource != null \
        ? oldSource.get("lastSync").get("effectiveAssignments")
        : sourceObject.get("lastSync").get("effectiveAssignments");
    JsonValue sourceObjectEffectiveAssignments = sourceObject.get("effectiveAssignments");

    if (cacheEffectiveAssignments(lastSyncEffectiveAssignments, sourceObjectEffectiveAssignments)) {
        def patch = [["operation" : "replace",
                      "field" : "/lastSync/"+ mappingName,
                      "value" : object(
                                    field("effectiveAssignments", sourceObjectEffectiveAssignments),
                                    field("timestamp", DateUtil.getDateUtil().now()))]];

        syncContext.disableSync()
        JsonValue patched = openidm.patch(
                resourcePath(mappingSource).child(sourceObject.get("_id").asString()).toString(), null, patch);
        source.put("_rev", patched.get("_rev").asString());
    }
} finally {
    if (syncContext != null) {
        syncContext.enableSync();
    }
}

/**
 * Determine if effective assignments are used and
 * check whether we should cache the effective assignments
 * in the lastSync attribute of managed/user if so.
 *
 * @return true to cache; false otherwise
 */
private boolean cacheEffectiveAssignments(JsonValue lastSyncEffectiveAssignments,
        JsonValue sourceObjectEffectiveAssignments ) {
    return sourceObjectEffectiveAssignments.isNotNull() \
        && !sameAssignments(lastSyncEffectiveAssignments.copy(), sourceObjectEffectiveAssignments.copy())
}


/**
 * Determine if the assignments in the array of assignments
 * are the same.
 *
 * @param lastSyncEA lastSyncEffectiveAssignments JsonValue.
 * @param sourceObjectEA sourceObjectEffectiveAssignments JsonValue.
 * @return true is they are the same; false otherwise
 */
private boolean sameAssignments(JsonValue lastSyncEA, JsonValue sourceObjectEA) {
    return lastSyncEA.asSet().equals(sourceObjectEA.asSet())
}