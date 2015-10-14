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
package org.forgerock.openidm.maintenance.upgrade;

/**
 * FileState reports the state of files in an original distribution as the compare to a new distribution.
 */
public enum FileState {
    /** The file exists on disk but not in the list of known files for the original distribution. */
    UNEXPECTED,
    /** The file does not exist on disk nor in the list of known files for the original distribution. */
    NONEXISTENT,
    /** The file should exist but does not. */
    DELETED,
    /** The file on disk has been changed since the original deployment. */
    DIFFERS,
    /** The file is unchanged from the original distribution. */
    UNCHANGED
}
