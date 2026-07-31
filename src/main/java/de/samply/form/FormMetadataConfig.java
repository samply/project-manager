package de.samply.form;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormMetadataConfig extends DisplayMetadata{

    private String title;

    // Groups are categories and subcategories used to organize form fields.
    private Map<String, DisplayMetadata> groups;

    private FormFieldConfig[] fields;

    // A block is a collection of fields that are always displayed together.
    private FormFieldBlock[] blocks;

    /**
     * Describes how collections of fields should be arranged in the frontend.
     * By default, fields are displayed vertically, one after another. A layout can
     * arrange a consecutive sequence of fields in a more complex structure, such as
     * a table. All fields in the sequence must belong to the same block, when a block
     * is defined, or otherwise to the same group and subgroup hierarchy.
     * <p>
     * For example, the following defines a two-column table with two rows,
     * followed by a three-column table with one row:
     * <pre>{@code
     * "layouts": [
     *   {
     *     "rows": [
     *       {"fields": ["field-label1", "field-label2"]},
     *       {"fields": ["field-label3", "field-label4"]}
     *     ]
     *   },
     *   {
     *     "rows": [
     *       {"fields": ["field-label5", "field-label6", "field-label7"]}
     *     ]
     *   }
     * ]
     * }</pre>
     */
    private FormFieldLayout[] layouts;

}
