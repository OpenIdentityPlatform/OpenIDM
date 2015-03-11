/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.smartevent;

/**
 * Represents an event entry that spans the start/end event. It allows the
 * calling of end() to complete the event window, and additional info useful for
 * monitoring such as a result to associate with the event.
 * 
 */

public interface EventEntry {

    /**
     * For events that mark/span the beginning and end of something, call this
     * method to mark the completion of the event window.
     */
    void end();

    /**
     * Optionally event consumer(s) can set a result to associate with the event
     * e.g. for monitoring purposes
     */
    void setResult(Object result);
}
