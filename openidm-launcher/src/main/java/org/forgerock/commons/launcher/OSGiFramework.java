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

import org.osgi.framework.Bundle;

/**
 * A Service expose the control of the OSGi Container.
 * 
 * @author Laszlo Hordos
 */
public interface OSGiFramework {

    public static final String LAUNCHER_INSTALL_LOCATION = "launcher.install.location";
    public static final String LAUNCHER_INSTALL_URL = "launcher.install.url";
    public static final String LAUNCHER_WORKING_LOCATION = "launcher.working.location";
    public static final String LAUNCHER_WORKING_URL = "launcher.working.url";
    public static final String LAUNCHER_PROJECT_LOCATION = "launcher.project.location";
    public static final String LAUNCHER_PROJECT_URL = "launcher.project.url";

    /**
     * Initializes this <code>Daemon</code> instance.
     * <p>
     * This method gets called once the JVM process is created and the
     * <code>Daemon</code> instance is created thru its empty public
     * constructor.
     * </p>
     * <p>
     * Under certain operating systems (typically Unix based operating systems)
     * and if the native invocation framework is configured to do so, this
     * method might be called with <i>super-user</i> privileges.
     * </p>
     * <p>
     * For example, it might be wise to create <code>ServerSocket</code>
     * instances within the scope of this method, and perform all operations
     * requiring <i>super-user</i> privileges in the underlying operating
     * system.
     * </p>
     * <p>
     * Apart from set up and allocation of native resources, this method must
     * not start the actual operation of the <code>Daemon</code> (such as
     * starting threads calling the <code>ServerSocket.accept()</code> method)
     * as this would impose some serious security hazards. The start of
     * operation must be performed in the <code>start()</code> method.
     * </p>
     * 
     * @param arguments
     *            A <code>DaemonContext</code> object used to communicate with
     *            the container.
     * @exception Exception
     *                Any exception preventing a successful initialization.
     */
    public void init(String[] arguments) throws Exception;

    /**
     * Starts the operation of this <code>Daemon</code> instance. This method is
     * to be invoked by the environment after the init() method has been
     * successfully invoked and possibly the security level of the JVM has been
     * dropped. Implementors of this method are free to start any number of
     * threads, but need to return control after having done that to enable
     * invocation of the stop()-method.
     */
    public void start() throws Exception;

    /**
     * Stops the operation of this <code>Daemon</code> instance. Note that the
     * proper place to free any allocated resources such as sockets or file
     * descriptors is in the destroy method, as the container may restart the
     * Daemon by calling start() after stop().
     */
    public void stop() throws Exception;

    /**
     * Frees any resources allocated by this daemon such as file descriptors or
     * sockets. This method gets called by the container after stop() has been
     * called, before the JVM exits. The Daemon can not be restarted after this
     * method has been called without a new call to the init() method.
     */
    public void destroy();

    /**
     * Get the system bundle
     * 
     * @return the System Bundle or <tt>null</tt> if the service is not running
     *         or the {@link #stop()} has been called before.
     */
    Bundle getSystemBundle();

}
