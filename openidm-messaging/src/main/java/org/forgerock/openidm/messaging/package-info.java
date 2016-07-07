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
 * Copyright 2016 ForgeRock AS.
 */

/**
 * OpenIDM Messaging Service.
 * <p>
 * This package holds the MessagingService and, within sub-packages, the different implementations of
 * MessageSubscribers and MessageHandlers.
 *
 * New Implementations of handlers and subscribers, and any support classes, should be contained in their own packages
 * based on the Message type they work with.
 *
 * The ScriptedMessageHandler is generic. It can handle any Message type.
 */
package org.forgerock.openidm.messaging;
