package de.samply.form;

/**
 * Identifies how a configured form-field entry is handled.
 *
 * <p>{@link #DYNAMIC} is the default and retains the existing behavior: the
 * field has its own configurable value and can be persisted as a project form
 * field. {@link #FIXED} is a metadata-only reference to a field implemented by
 * the frontend. A fixed entry can control display metadata, order and active
 * state, but it must never be handled or persisted as a dynamic form field.</p>
 */
public enum FormFieldType {
    DYNAMIC,
    FIXED
}
