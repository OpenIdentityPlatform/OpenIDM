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

package org.forgerock.openidm.internal.recon;

/**
 * A NAME does ...
 * 
 * <pre>
 *  t       s
 *  a       o
 *  r   l   u
 *  g   i   r
 *  e   n   c
 *  t   k   e
 *  ---------
 *  1   1   1   =   7   CONFIRMED
 *  1   1   0   =   6   SOURCE_MISSING
 *  1   0   1   =   5   FOUND
 *  1   0   0   =   4   UNASSIGNED
 *  0   1   1   =   3   MISSING
 *  0   1   0   =   2   LINK_ONLY
 *  0   0   1   =   1   ABSENT
 *  0   0   0   =   0   ALL_GONE
 * 
 * 
 * </pre>
 * 
 * source target
 * 
 * @author Laszlo Hordos
 */
public enum ReconSituation {

    UNKNOWN(8), ALL_GONE(0), ABSENT(1), LINK_ONLY(2), MISSING(3), UNASSIGNED(4), FOUND(5),
    SOURCE_MISSING(6), CONFIRMED(7);

    private final int value;

    private ReconSituation(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ReconSituation from(int intValue) {
        switch (intValue) {
        case 0:
            return ALL_GONE;
        case 1:
            return ABSENT;
        case 2:
            return LINK_ONLY;
        case 3:
            return MISSING;
        case 4:
            return UNASSIGNED;
        case 5:
            return FOUND;
        case 6:
            return SOURCE_MISSING;
        case 7:
            return CONFIRMED;
        default:
            return UNKNOWN;
        }
    }
}
