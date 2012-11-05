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

import java.net.URL;
import java.util.EnumSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * A BundleHandler contains the instruction of how to provision {@link Bundle}
 * to the {@link org.osgi.framework.launch.Framework}.
 * 
 * @author Laszlo Hordos
 */
public class BundleHandler {

    public enum Action {
        /**
         * The name used for the install action.
         */
        install,
        /**
         * The name used for the start action.
         */
        start,
        /**
         * The name used for the update action.
         */
        update,
        /**
         * The name used for the uninstall action.
         */
        uninstall;
    }

    private URL bundleUrl;
    private EnumSet<Action> actions;
    private Integer startLevel;

    public BundleHandler(URL bundleUrl, EnumSet<Action> actions, Integer startLevel) {
        this.bundleUrl = bundleUrl;
        this.actions = actions;
        this.startLevel = startLevel;
    }

    public static boolean isNotFragment(Bundle bundle) {
        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null;
    }

    /**
     * Get the bundle location URL.
     * 
     * @return the URL of the Bundle location.
     */
    public URL getBundleUrl() {
        return bundleUrl;
    }

    public Integer getStartLevel() {
        return startLevel;
    }

    public EnumSet<Action> getActions() {
        return actions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("BundleHandler[");
        sb.append("url: ").append(bundleUrl);
        if (null != actions) {
            sb.append(" action: ").append(actions);
        }
        if (null != startLevel) {
            sb.append(" startLevel: ").append(startLevel);
        }
        sb.append("]");
        return sb.toString();
    }
}
