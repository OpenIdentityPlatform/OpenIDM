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

import static ch.qos.logback.classic.Level.*
import static ch.qos.logback.core.spi.FilterReply.DENY
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL
import ch.qos.logback.classic.boolex.GEventEvaluator
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.filter.EvaluatorFilter
import org.forgerock.openidm.core.IdentityServer

def patternExpression = "%date{ISO8601} [%5level] %msg%n"
def stdPattern ="%-4relative [%thread] %-5level %logger{35} - %msg %n"
def filePattern = "%date %level [%thread] %logger{10} [%file:%line] %msg%n"

def logFolder = IdentityServer.getFileForInstallPath("logs").absolutePath

println logFolder

scan()
appender("FILE", FileAppender) {
    file = "${logFolder}/logs/openidm.log"
    encoder(PatternLayoutEncoder) {
        pattern = filePattern
    }
}

appender("STDERR", ConsoleAppender) {
    filter(EvaluatorFilter) {
        evaluator(GEventEvaluator) {
            expression = 'e.level.toInt() >= WARN.toInt()'
        }
        onMatch = NEUTRAL
        onMismatch = DENY
    }
    encoder(PatternLayoutEncoder) {
        pattern = patternExpression
    }
    target = "System.err"
}

appender("STDOUT", ConsoleAppender) {
    filter(EvaluatorFilter) {
        evaluator(GEventEvaluator) {
            expression = 'e.level.toInt() < WARN.toInt()'
        }
        onMismatch = DENY
        onMatch = NEUTRAL
    }
    encoder(PatternLayoutEncoder) {
        pattern = patternExpression
    }
    target = "System.out"
}

logger("org.forgerock.openidm.provisioner.openicf", WARN)
logger("org.forgerock.openidm.managed", WARN)
logger("org.forgerock.openidm.repo", WARN)

root(INFO,["FILE","STDERR","STDOUT"])
