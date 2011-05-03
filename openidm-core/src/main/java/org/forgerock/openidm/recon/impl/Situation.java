/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.recon.impl;

/**
 * An enumeration of possible Situation conditions that may apply during
 * reconciliation.
 * <p/>
 * <ul>
 * <li>CONFIRMED the object was expected to exist at the target and it does</li>
 * <li>DELETED the object was expected to exist at the target but it doesn't</li>
 * <li>UNMATCHED there is no matching target object after correlation, but there is at the source</li>
 * <li>UNASSIGNED there is a matching object on the target and source, but there is no link established</li>
 * <li>DISPUTED there are several matching target or source objects</li>
 * <li>PENDING a situation that indicates there are pending changes</li>
 * <ul>
 * <p/>
 * A {@code reconciliation policy} maps these situation conditions to scripts, which evaluate
 * and return an appropriate action for the {@link ReconciliationServiceImpl} to take.
 */
public enum Situation {

    CONFIRMED,
    DELETED,
    UNMATCHED,
    UNASSIGNED,
    DISPUTED,
    PENDING

}