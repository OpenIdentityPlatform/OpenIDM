/**
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
 * Copyright 2011-2016 ForgeRock AS.
 */

define("config/main", [
    "./routes/CommonRoutesConfig",
    "./routes/SelfServiceRoutesConfig",
    "./routes/UserRoutesConfig",
    "./routes/CommonIDMRoutesConfig",
    "./messages/CommonMessages",
    "./messages/CommonIDMMessages",
    "./messages/SelfServiceMessages",
    "./messages/UserMessages",
    "./validators/CommonValidators",
    "./validators/SelfServiceValidators",
    "./AppConfiguration",
    "./process/CommonIDMConfig",
    "./process/IDMConfig",
    "./process/CommonConfig",
    "./errorhandlers/CommonErrorHandlers"
]);
