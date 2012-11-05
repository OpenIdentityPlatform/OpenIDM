/*
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
 */

package org.forgerock.commons.launcher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.StringTokenizer;

import org.forgerock.json.fluent.JsonValue;

/**
 * A BundleHandlerBuilder builds {@link BundleHandler} instances based on the
 * given configuration.
 * 
 * @author Laszlo Hordos
 */
public class BundleHandlerBuilder {

    private final EnumSet<BundleHandler.Action> actions;
    private final Integer startLevel;

    public BundleHandlerBuilder(EnumSet<BundleHandler.Action> actions, Integer startLevel) {
        this.actions = actions;
        this.startLevel = startLevel;
    }

    public static BundleHandlerBuilder newBuilder(JsonValue configuration,
            BundleHandlerBuilder defaultTo) {
        if (configuration.isDefined("start-level") || configuration.isDefined("action")) {
            return newBuilder(configuration.get("start-level").asInteger(), configuration.get(
                    "action").asString());
        }
        return null != defaultTo ? defaultTo : newBuilder((Integer) null);
    }

    public static BundleHandlerBuilder newBuilder(JsonValue configuration) {
        return newBuilder(configuration, null);
    }

    public static BundleHandlerBuilder newBuilder(Integer startLevel) {
        return new BundleHandlerBuilder(EnumSet.of(BundleHandler.Action.install,
                BundleHandler.Action.start), startLevel);
    }

    public static BundleHandlerBuilder newBuilder(Integer startLevel, String action) {
        EnumSet<BundleHandler.Action> actionSet = null;
        if (null != action && action.trim().length() > 0) {
            actionSet = EnumSet.noneOf(BundleHandler.Action.class);
            StringTokenizer tokens = new StringTokenizer(action, ".");
            while (tokens.hasMoreTokens()) {
                actionSet.add(BundleHandler.Action.valueOf(tokens.nextToken()));
            }
        }
        return new BundleHandlerBuilder(actionSet != null ? actionSet : EnumSet.of(
                BundleHandler.Action.install, BundleHandler.Action.start), startLevel);
    }

    public BundleHandler build(URL bundleURL) {
        return new BundleHandler(bundleURL, actions, startLevel);
    }

    public BundleHandler build(String bundleURL) throws MalformedURLException {
        return build(new URL(bundleURL));
    }

    public boolean equals(BundleHandlerBuilder that) {
        if (this == that)
            return true;
        if ((that == null)
                || (!actions.equals(that.actions) || !startLevel.equals(that.startLevel)))
            return false;
        return true;
    }

}
