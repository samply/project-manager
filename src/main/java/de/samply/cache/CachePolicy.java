package de.samply.cache;

/**
 * Named cache policies supported by Project Manager.
 *
 * <p>Keeping these values explicit prevents the configuration from accepting
 * arbitrary Cache-Control header values. The durations for SHORT and LONG are
 * supplied by {@link CacheConfiguration}.</p>
 */
public enum CachePolicy {
    /** Do not store the response anywhere. */
    NO_STORE,
    /** A stored response must be revalidated before it is reused. */
    NO_CACHE,
    /** Public response with the configured short max-age. */
    SHORT,
    /** Public response with the configured long max-age. */
    LONG,
    /** Public response that may be stored for a year without revalidation. */
    IMMUTABLE
}
