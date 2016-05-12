/*
            return HttpUtils.
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
 * Portions copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

/**
 * A Utils class is a collection of common methods used in the script library.
 *
 */
public class Utils {

    public static class Pair<A, B> {
        public final A fst;
        public final B snd;

        public Pair(final A a, final B b) {
            fst = a;
            snd = b;
        }

        public String toString() {
            return "Pair[" + fst + "," + snd + "]";
        }

        private static boolean equals(Object x, Object y) {
            return (x == null && y == null) || (x != null && x.equals(y));
        }

        public boolean equals(Object other) {
            return other instanceof Pair<?, ?> && equals(fst, ((Pair<?, ?>) other).fst)
                    && equals(snd, ((Pair<?, ?>) other).snd);
        }

        public int hashCode() {
            int result = fst != null ? fst.hashCode() : 0;
            result = 31 * result + (snd != null ? snd.hashCode() : 0);
            return result;
        }

        public static <A, B> Pair<A, B> of(final A a, final B b) {
            return new Pair<A, B>(a, b);
        }
    }

    private Utils() {
        // prevent instantiation
    }

}
