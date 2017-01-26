/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2017 ForgeRock AS.
 */

package org.forgerock.openidm.maintenance.upgrade;

/**
 * A version range is an interval describing a set of {@link Version versions}.
 * <p/>
 * A range has a left (lower) endpoint and a right (upper) endpoint. Each
 * endpoint can be open (excluded from the set) or closed (included in the set).
 *
 * <p>
 * {@code VersionRange} objects are immutable.
 * <p>
 * Class copied from ICF 1.5
 */
public class VersionRange {

    /**
     * The left endpoint is open and is excluded from the range.
     * <p>
     * The value of {@code LEFT_OPEN} is {@code '('}.
     */
    public static final char LEFT_OPEN = '(';
    /**
     * The left endpoint is closed and is included in the range.
     * <p>
     * The value of {@code LEFT_CLOSED} is {@code '['}.
     */
    public static final char LEFT_CLOSED = '[';
    /**
     * The right endpoint is open and is excluded from the range.
     * <p>
     * The value of {@code RIGHT_OPEN} is {@code ')'}.
     */
    public static final char RIGHT_OPEN = ')';
    /**
     * The right endpoint is closed and is included in the range.
     * <p>
     * The value of {@code RIGHT_CLOSED} is {@code ']'}.
     */
    public static final char RIGHT_CLOSED = ']';

    private static final String ENDPOINT_DELIMITER = ",";

    private final Version floorVersion;
    private final boolean isFloorInclusive;
    private final Version ceilingVersion;
    private final boolean isCeilingInclusive;
    private final boolean empty;

    /**
     * Parse version component into a Version.
     *
     * @param version
     *            version component string
     * @param range
     *            Complete range string for exception message, if any
     * @return Version
     */
    private static Version parseVersion(String version, String range) {
        try {
            return Version.parse(version);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "invalid range \"" + range + "\": " + e.getMessage(), e);
        }
    }

    /**
     * Creates a version range from the specified string.
     *
     * <p>
     * Version range string grammar:
     *
     * <pre>
     * range ::= interval | at least
     * interval ::= ( '[' | '(' ) left ',' right ( ']' | ')' )
     * left ::= version
     * right ::= version
     * at least ::= version
     * </pre>
     *
     * @param range
     *            String representation of the version range. The versions in
     *            the range must contain no whitespace. Other whitespace in the
     *            range string is ignored.
     * @throws IllegalArgumentException
     *             If {@code range} is improperly formatted.
     */
    public static VersionRange parse(String range) {
        if (range == null || range.trim().length() == 0) {
            throw new IllegalArgumentException("Range cannot be empty");
        }
        int idx = range.indexOf(ENDPOINT_DELIMITER);
        // Check if the version is an interval.
        if (idx > 1 && idx == range.lastIndexOf(ENDPOINT_DELIMITER)) {
            String vlo = range.substring(0, idx).trim();
            String vhi = range.substring(idx + 1).trim();

            boolean isLowInclusive = true;
            boolean isHighInclusive = true;
            if (vlo.charAt(0) == LEFT_OPEN) {
                isLowInclusive = false;
            } else if (vlo.charAt(0) != LEFT_CLOSED) {
                throw new IllegalArgumentException("invalid range \"" + range
                        + "\": invalid format");
            }
            vlo = vlo.substring(1).trim();

            if (vhi.charAt(vhi.length() - 1) == RIGHT_OPEN) {
                isHighInclusive = false;
            } else if (vhi.charAt(vhi.length() - 1) != RIGHT_CLOSED) {
                throw new IllegalArgumentException("invalid range \"" + range
                        + "\": invalid format");
            }
            vhi = vhi.substring(0, vhi.length() - 1).trim();

            return new VersionRange(parseVersion(vlo, range), isLowInclusive, parseVersion(vhi,
                    range), isHighInclusive);
        } else if (idx == -1) {
            return new VersionRange(VersionRange.parseVersion(range.trim(), range), true, null,
                    false);
        } else {
            throw new IllegalArgumentException("invalid range \"" + range + "\": invalid format");
        }
    }

    public VersionRange(Version low, boolean isLowInclusive, Version high, boolean isHighInclusive) {
        if (low == null) {
            throw new IllegalArgumentException("Floor version cannot be null");
        }
        floorVersion = low;
        isFloorInclusive = isLowInclusive;
        ceilingVersion = high;
        isCeilingInclusive = isHighInclusive;
        empty = isEmpty0();
    }

    public Version getFloor() {
        return floorVersion;
    }

    public boolean isFloorInclusive() {
        return isFloorInclusive;
    }

    public Version getCeiling() {
        return ceilingVersion;
    }

    public boolean isCeilingInclusive() {
        return isCeilingInclusive;
    }

    public boolean isInRange(Version version) {
        if (empty) {
            return false;
        }
        int c = floorVersion.compareTo(version);
        if (c == 0 && isFloorInclusive){
            return true;
        } else if (c < 0 && ceilingVersion != null) {
            return ceilingVersion.compareTo(version) >= (isCeilingInclusive ? 0 : 1);
        }
        return false;
    }

    /**
     * Returns whether this version range contains only a single version.
     *
     * @return {@code true} if this version range contains only a single
     *         version; {@code false} otherwise.
     */
    public boolean isExact() {
        if (empty) {
            return false;
        } else if (ceilingVersion == null) {
            return true;
        }
        if (isFloorInclusive) {
            if (isCeilingInclusive) {
                // [f,c]: exact if f == c
                return floorVersion.equals(ceilingVersion);
            } else {
                // [f,c): exact if f++ >= c
                Integer major = floorVersion.getMajor();
                Integer minor = floorVersion.getMinor();
                Integer micro = floorVersion.getMicro();
                Integer revision = floorVersion.getRevision();
                Version adjacent1 =
                        new Version(major != null ? major : 0, minor != null ? minor : 0,
                                micro != null ? micro : 0, revision != null ? revision + 1 : 1);
                return adjacent1.compareTo(ceilingVersion) >= 0;
            }
        } else {
            if (isCeilingInclusive) {
                // (f,c] is equivalent to [f++,c]: exact if f++ == c
                Version adjacent1 =
                        new Version(floorVersion.getMajor(), floorVersion.getMinor(), floorVersion
                                .getMicro(), floorVersion.getRevision() + 1);
                return adjacent1.equals(ceilingVersion);
            } else {
                // (f,c) is equivalent to [f++,c): exact if (f++)++ >=c
                Version adjacent2 =
                        new Version(floorVersion.getMajor(), floorVersion.getMinor(), floorVersion
                                .getMicro(), floorVersion.getRevision() + 2);
                return adjacent2.compareTo(ceilingVersion) >= 0;
            }
        }
    }

    /**
     * Returns whether this version range is empty. A version range is empty if
     * the set of versions defined by the interval is empty.
     *
     * @return {@code true} if this version range is empty; {@code false}
     *         otherwise.
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Internal isEmpty behavior.
     *
     * @return {@code true} if this version range is empty; {@code false}
     *         otherwise.
     */
    private boolean isEmpty0() {
        if (ceilingVersion == null) { // infinity
            return false;
        }
        int comparison = floorVersion.compareTo(ceilingVersion);
        if (comparison == 0) { // endpoints equal
            return !isFloorInclusive || !isCeilingInclusive;
        }
        return comparison > 0; // true if left > right
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VersionRange other = (VersionRange) obj;
        if (floorVersion != other.floorVersion
                && (floorVersion == null || !floorVersion.equals(other.floorVersion))) {
            return false;
        }
        if (isFloorInclusive != other.isFloorInclusive) {
            return false;
        }
        if (ceilingVersion != other.ceilingVersion
                && (ceilingVersion == null || !ceilingVersion.equals(other.ceilingVersion))) {
            return false;
        }
        if (isCeilingInclusive != other.isCeilingInclusive) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = floorVersion.hashCode();
        result = 31 * result + (isFloorInclusive ? 1 : 0);
        result = 31 * result + (ceilingVersion != null ? ceilingVersion.hashCode() : 0);
        result = 31 * result + (isCeilingInclusive ? 1 : 0);
        return result;
    }

    public String toString() {
        if (ceilingVersion != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(isFloorInclusive ? LEFT_CLOSED : LEFT_OPEN);
            sb.append(floorVersion.getVersion()).append(ENDPOINT_DELIMITER).append(
                    ceilingVersion.getVersion());
            sb.append(isCeilingInclusive ? RIGHT_CLOSED : RIGHT_OPEN);
            return sb.toString();
        } else {
            return floorVersion.getVersion();
        }
    }
}
