package de.samply.form;

/**
 * Identifies how a configured form-field entry is handled.
 *
 * <p>{@link #DYNAMIC} is the default and retains the existing behavior: the
 * field has its own configurable value and can be persisted as a project form
 * field. {@link #FIXED} is a metadata-only reference to a field implemented by
 * the frontend. A fixed entry can control the native field's display metadata,
 * configured order, active state, and conditional visibility. A false
 * condition is represented as {@code active=false}; the fixed metadata remains
 * in the response so the frontend can suppress the native field. Fixed entries
 * must never be handled or persisted as dynamic form fields.</p>
 */
public enum FormFieldType {
    DYNAMIC,
    FIXED
}
