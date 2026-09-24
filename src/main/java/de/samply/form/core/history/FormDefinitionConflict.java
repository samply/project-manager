package de.samply.form.core.history;

/**
 * An incompatible change between a form's recorded definition and its current
 * configuration. {@code label} is null for a change of the whole form.
 */
public record FormDefinitionConflict(String formTitle, String label, Kind kind, String oldValue, String newValue) {

    public enum Kind {
        FORM_REMOVED("form removed or renamed"),
        FIELD_REMOVED("field removed"),
        FIELD_TYPE_CHANGED("field_type changed"),
        DATA_TYPE_CHANGED("data_type changed"),
        ALLOWED_VALUES_REMOVED("allowed values removed or renamed"),
        BLOCK_CHANGED("block changed"),
        BLOCK_MULTIPLE_DISABLED("block multiple changed from true to false"),
        MULTIPLE_DISABLED("multiple changed from true to false"),
        AS_FILE_CHANGED("as_file changed");

        private final String description;

        Kind(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /** One line for the log, e.g. {@code Form "ethics", field "status": data_type changed: STRING -> ENUM}. */
    public String describe() {
        String where = label == null ? "Form \"" + formTitle + "\"" : "Form \"" + formTitle + "\", field \"" + label + "\"";
        String change;
        if (oldValue == null && newValue == null) {
            change = "";
        } else if (newValue == null && (kind == Kind.ALLOWED_VALUES_REMOVED || kind == Kind.BLOCK_MULTIPLE_DISABLED)) {
            change = ": " + oldValue;
        } else {
            change = ": " + orNone(oldValue) + " -> " + orNone(newValue);
        }
        return where + ": " + kind.getDescription() + change;
    }

    private static String orNone(String value) {
        return value != null ? value : "none";
    }
}
