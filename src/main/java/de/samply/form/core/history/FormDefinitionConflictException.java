package de.samply.form.core.history;

import java.util.List;

/** Thrown at startup when forms were changed incompatibly; stops the project manager. */
public class FormDefinitionConflictException extends RuntimeException {

    private final transient List<FormDefinitionConflict> conflicts;

    public FormDefinitionConflictException(List<FormDefinitionConflict> conflicts) {
        super(conflicts.size() + " incompatible form definition change(s) - see the log above: "
                + String.join("; ", conflicts.stream().map(FormDefinitionConflict::describe).toList()));
        this.conflicts = List.copyOf(conflicts);
    }

    public List<FormDefinitionConflict> getConflicts() {
        return conflicts;
    }
}
