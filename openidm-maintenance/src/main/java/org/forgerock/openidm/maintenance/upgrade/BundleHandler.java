/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openidm.maintenance.upgrade;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.nio.file.Path;

/**
 * Designed to use the systemBundle from the OSGI framework to handle
 * the actions that can be taken on a {@link org.osgi.framework.Bundle}.
 */
public class BundleHandler {

    private BundleContext context;

    public BundleHandler(BundleContext context) {
        this.context = context;
    }

    /**
     *  Update a current installed bundle.
     * @param bundle that will be updated
     * @throws UpgradeException
     */
    public void updateBundle(Bundle bundle) throws UpgradeException {
        try {
            bundle.update();
        } catch (BundleException e) {
            throw new UpgradeException("Cannot update bundle " + bundle.toString(), e);
        }
    }

    /**
     *  Stop a currently running bundle.
     * @param bundle that will be stopped
     * @throws UpgradeException
     */
    public void stopBundle(Bundle bundle) throws UpgradeException {
        try {
            bundle.stop();
        } catch (BundleException e) {
            throw new UpgradeException("Cannot stop bundle " + bundle.toString(), e);
        }
    }

    /**
     * Uninstall a currently installed bundle.
     * @param bundle that will be uninstalled
     * @throws UpgradeException
     */
    public void uninstallBundle(Bundle bundle) throws UpgradeException {
        try {
            bundle.uninstall();
        } catch (BundleException e) {
            throw new UpgradeException("Cannot uninstall bundle " + bundle.toString(), e);
        }
    }

    /**
     * Installs a bundle in the OSGI Framework.
     * @param path to bundle which will be installed
     * @throws UpgradeException
     */
    public void installBundle(Path path) throws UpgradeException {
        try {
            context.installBundle(path.toString());
        } catch (BundleException e) {
            throw new UpgradeException("Cannot install bundle " + path.toString(), e);
        }

    }

    /**
     * Start the bundle.
     * @param bundle to be started
     * @throws UpgradeException
     */
    public void startBundle(Bundle bundle) throws UpgradeException{
        try {
            bundle.start();
        } catch (BundleException e) {
            throw new UpgradeException("Cannot start bundle " + bundle.toString(), e);
        }
    }
}
