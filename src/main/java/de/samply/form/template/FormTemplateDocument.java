package de.samply.form.template;

import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormFieldGroup;
import de.samply.form.template.FormFieldLayoutResolver.KeyedField;

import java.util.List;

/**
 * The structure of a generated form, built in Java so the Thymeleaf template only
 * renders it: an ordered list of nodes, each a section heading (one per printed
 * form), a group heading, a block-instance heading, a row of one or more
 * fields, or a checkbox list. Rows before the first section heading form the
 * header block.
 * <p>
 * Nodes expose a {@code kind()} the template switches on, since Thymeleaf's
 * restricted SpEL cannot test a node's Java type. Descriptions are already
 * resolved: the full one, else the short one.
 */
public record FormTemplateDocument(List<Node> nodes) {

    public sealed interface Node permits SectionHeading, GroupHeading, BlockHeading, FieldRow, CheckBoxList {
        String kind();
    }

    /** Heading of a printed form, from the form's own display metadata. */
    public record SectionHeading(Form form, String description) implements Node {
        @Override
        public String kind() {
            return "section";
        }
    }

    /**
     * Heading of a field group, from the group's own metadata. {@code level}
     * is its depth: 1 for a field's first group, 2 for its second, and so on.
     */
    public record GroupHeading(FormFieldGroup group, int level, String description) implements Node {
        @Override
        public String kind() {
            return "group";
        }
    }

    /**
     * Heading of a block instance, e.g. "Collaborator 2". {@code description}
     * is set only for the first instance of a block, since it describes the
     * block in general rather than one instance.
     */
    public record BlockHeading(String title, String description) implements Node {
        @Override
        public String kind() {
            return "block";
        }
    }

    /**
     * One table row: a single field, or ({@code layout}) the fields of a
     * configured layout row placed side by side, each taking {@code widths}
     * percent of its cell. {@code fullWidth}: a single field without display
     * name and description, whose value takes the whole row, as in the
     * frontend Summary.
     */
    public record FieldRow(List<KeyedField> fields, boolean layout, List<Integer> widths, boolean fullWidth)
            implements Node {
        @Override
        public String kind() {
            return "row";
        }
    }

    /**
     * A CHECK_BOX field: every allowed value with its own checkbox, checked if
     * selected - so an unchecked option (e.g. a declaration) stays visible.
     * {@code field} carries the label; {@code fullWidth} as in {@link FieldRow}.
     */
    public record CheckBoxList(KeyedField field, List<CheckBoxOption> options, boolean fullWidth) implements Node {
        @Override
        public String kind() {
            return "checkboxes";
        }
    }

    /** One option of a {@link CheckBoxList}; {@code name} is its form-widget name. */
    public record CheckBoxOption(String name, String displayName, String description, boolean checked) {
    }
}
