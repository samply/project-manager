package de.samply.project.code;

/** Tokens supported by {@code PROJECT_ID_TEMPLATE}. */
public enum ProjectCodeToken {
    /** Four-digit calendar year. */
    YEAR(false, false),
    /** Two-digit calendar month. */
    MONTH(false, false),
    /** Two-digit day of the month. */
    DAY(false, false),
    /** Two-digit hour in the application time zone. */
    HOUR(false, false),
    /** Two-digit minute. */
    MINUTE(false, false),
    /** Two-digit second. */
    SECOND(false, false),
    /** Random decimal digits with the requested length. */
    NUMBER(true, true),
    /** Database sequence value, zero-padded to at least the requested width. */
    SEQUENCE(true, true),
    /** Random lowercase hexadecimal characters with the requested length. */
    STRING(true, true);

    private final boolean lengthRequired;
    private final boolean distinguishing;

    ProjectCodeToken(boolean lengthRequired, boolean distinguishing) {
        this.lengthRequired = lengthRequired;
        this.distinguishing = distinguishing;
    }

    /** Whether the token must be written as {@code {{TOKEN:n}}}; other tokens take no length. */
    public boolean isLengthRequired() {
        return lengthRequired;
    }

    /** Whether the token produces a different value for projects created at the same time. */
    public boolean isDistinguishing() {
        return distinguishing;
    }

}
