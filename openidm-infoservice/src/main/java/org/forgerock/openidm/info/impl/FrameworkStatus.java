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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.info.impl;

import org.osgi.framework.FrameworkEvent;

/**
 * A Framework Status class for storing the framework status indicated
 * by the last {@link org.osgi.framework.FrameworkEvent} event published.
 */
public class FrameworkStatus {

	/**
     * A framework status instance holder
     */
    private static class InstanceHolder {
        private static final FrameworkStatus instance = new FrameworkStatus(-1);
    }
    
    /**
     * An integer representing the framework status.
     * See {@link org.osgi.framework.FrameworkEvent} for 
     */
    private int frameworkStatus;

    /**
     * Constructor
     * 
     * @param frameworkStatus An integer representing the framework status
     */
    private FrameworkStatus(int frameworkStatus) {
        this.frameworkStatus = frameworkStatus;
    }
    
    /**
     * Gets an instance of the framework status.
     *
     * @return a FrameworkStatus instance
     */
    public static synchronized FrameworkStatus getInstance() {
        return InstanceHolder.instance;
    }
    
    /**
     * Returns the current framework status.
     * 
     * @return an integer representing the framework status.
     */
    public int getFrameworkStatus() {
    	return this.frameworkStatus;
    }
    
    /**
     * Sets the current framework status.
     * 
     * @param frameworkStatus an integer representing the framework status.
     */
    public void setFrameworkStatus(int frameworkStatus) {
    	this.frameworkStatus = frameworkStatus; 
    }
    
    /**
     * Returns true if the framework has been started and is any of the following 
     * states indicating it is ready:  STARTED, PACKAGES_REFRESHED, WARNING, INFO.
     * 
     * @return a boolean indicating if the framework is ready.
     */
    public boolean isReady() {
        return frameworkStatus == FrameworkEvent.STARTED
                || frameworkStatus == FrameworkEvent.PACKAGES_REFRESHED
                || frameworkStatus == FrameworkEvent.STARTLEVEL_CHANGED
                || frameworkStatus == FrameworkEvent.WARNING
                || frameworkStatus == FrameworkEvent.INFO;
    }
}
