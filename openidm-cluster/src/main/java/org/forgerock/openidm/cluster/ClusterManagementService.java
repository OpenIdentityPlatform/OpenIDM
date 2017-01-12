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
 * Portions copyright 2013-2017 ForgeRock AS.
 */
package org.forgerock.openidm.cluster;

/**
 * An interface for the OpenIDM Cluster Management Service
 * 
 */
public interface ClusterManagementService {

    /**
     * Registers a listener for notifications of cluster events
     * 
     * @param listenerId the listener's ID
     * @param listener  the listener implementation
     */
    public void register(String listenerId, ClusterEventListener listener);

    /**
     * Unregisters a listener
     * 
     * @param listenerId the listener's ID
     */
    public void unregister(String listenerId);
    
    /**
     * Sends a ClusterEvent to the other nodes in the cluster.
     * 
     * @param event the event to publish
     */
    public void sendEvent(ClusterEvent event);
    
    /**
     * Renews the Cluster Management Service's recovery lease. This should
     * be used by listener's to prevent the recovery from timing out during
     * the processing of an recovery event.
     * 
     * @param instanceId the ID of the instance being recovered
     */
    public void renewRecoveryLease(String instanceId);
    
    /**
     * Starts the cluster management thread.
     */
    public void startClusterManagement();

    /**
     * Returns true if the cluster mangement thread has started, false
     * otherwise.
     */
    public boolean isStarted();
    
    /**
     * Returns true if clustering is enabled, false otherwise.
     */
    public boolean isEnabled();
    
    /**
     * Returns the node's instance ID
     */
    public String getInstanceId();
}
