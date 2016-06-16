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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.commons.launcher;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * <p>
 * This class is the default way to instantiate and execute the framework. It is
 * not intended to be the only way to instantiate and execute the framework;
 * rather, it is one example of how to do so. When embedding the framework in a
 * host application, this class can serve as a simple guide of how to do so.
 * </p>
 */
public class Main {

    private static OSGiFrameworkService _server = null;

    public static void main(String[] args) throws Exception {
        OSGiFrameworkService server = new OSGiFrameworkService();
        server.init(args);
        server.start();
        if (!server.isNewThread()) {
            // The Framework was started by the Main thread and the Framework
            // was stopped.
            // Some Bundle may keep thread and prevent the System from stopping
            if (server.restart.get()) {
                // Send -1 to indicate a restart to the start script
                System.exit(-1);
            }
            System.exit(0);
        }
    }

    /*
     * The start method should not return until the stop method has been called.
     */
    static void start(String[] args) throws Exception {
        if (null != _server) {
            throw new IllegalStateException("Server is already running.");
        }
        System.out.append("Starting ForgeRock Launcher: ").println(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
        _server = new OSGiFrameworkService();
        _server.init(args);
        _server.setNewThread(false);
        _server.start();

    }

    static void stop(String[] args) throws Exception {
        if (_server == null) {
            /* If procrun called stop() without calling start(). */
            System.err.println("Server has not been started yet");
            return;
        }
        System.out.append("Stopping ForgeRock Launcher: ").println(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
        /*
         * It seems that sometimes procrun will run the start method in a thread
         * with a null context class loader.
         */
        if (Thread.currentThread().getContextClassLoader() == null) {
            // getLog().warn("Context class loader is null, working around");
            Thread.currentThread().setContextClassLoader(Main.class.getClassLoader());
        }

        _server.stop();
        _server.destroy();

        /*
         * Do not set _server to null, because that way the check in star()
         * fails and we ensure that the server cannot be started twice in the
         * same JVM. Procrun does not call JNI_DestroyJavaVM(), so shutdown
         * hooks do not run. We reset the LM here.
         */
    }

}
