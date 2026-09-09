package de.samply.form.template;

import de.samply.form.FormFieldLayoutRow;
import de.samply.frontend.dto.FormField;
import de.samply.utils.FormFieldUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FormFieldLayoutResolverTest {

    @Test
    void resolvesNothingWhenNoLayoutsAreConfigured() {
        Map<String, FormField> fields = fieldsOf(field("samples", "volume", null));

        FormFieldLayoutResolver resolver = FormFieldLayoutResolver.resolve(fields, null);

        assertThat(resolver.rowFieldsStartingAt(keyOf(fields, "volume"))).isNull();
        assertThat(resolver.isSkippedRowMember(keyOf(fields, "volume"))).isFalse();
    }

    @Test
    void groupsACompleteRowAndSkipsItsNonFirstMembers() {
        FormField volume = field("samples", "volume", null);
        FormField unit = field("samples", "unit", null);
        FormField unrelated = field("samples", "comments", null);
        Map<String, FormField> fields = fieldsOf(volume, unit, unrelated);
        Map<String, List<FormFieldLayoutRow>> layouts = Map.of(
                "samples", List.of(new FormFieldLayoutRow(List.of("volume", "unit"))));

        FormFieldLayoutResolver resolver = FormFieldLayoutResolver.resolve(fields, layouts);

        String volumeKey = keyOf(fields, "volume");
        String unitKey = keyOf(fields, "unit");
        String unrelatedKey = keyOf(fields, "comments");

        assertThat(resolver.rowFieldsStartingAt(volumeKey))
                .extracting(FormFieldLayoutResolver.KeyedField::field)
                .containsExactly(volume, unit);
        assertThat(resolver.isSkippedRowMember(volumeKey)).isFalse();
        assertThat(resolver.isSkippedRowMember(unitKey)).isTrue();
        assertThat(resolver.rowFieldsStartingAt(unitKey)).isNull();
        assertThat(resolver.rowFieldsStartingAt(unrelatedKey)).isNull();
        assertThat(resolver.isSkippedRowMember(unrelatedKey)).isFalse();
    }

    @Test
    void doesNotGroupAnIncompleteRow() {
        FormField volume = field("samples", "volume", null);
        // "unit" is configured in the row but never present in this fields map.
        Map<String, FormField> fields = fieldsOf(volume);
        Map<String, List<FormFieldLayoutRow>> layouts = Map.of(
                "samples", List.of(new FormFieldLayoutRow(List.of("volume", "unit"))));

        FormFieldLayoutResolver resolver = FormFieldLayoutResolver.resolve(fields, layouts);

        String volumeKey = keyOf(fields, "volume");
        assertThat(resolver.rowFieldsStartingAt(volumeKey)).isNull();
        assertThat(resolver.isSkippedRowMember(volumeKey)).isFalse();
    }

    @Test
    void resolvesEachBlockInstanceOfARepeatableBlockIndependently() {
        FormField volume1 = field("samples", "volume", 1);
        FormField unit1 = field("samples", "unit", 1);
        FormField volume2 = field("samples", "volume", 2);
        FormField unit2 = field("samples", "unit", 2);
        Map<String, FormField> fields = fieldsOf(volume1, unit1, volume2, unit2);
        Map<String, List<FormFieldLayoutRow>> layouts = Map.of(
                "samples", List.of(new FormFieldLayoutRow(List.of("volume", "unit"))));

        FormFieldLayoutResolver resolver = FormFieldLayoutResolver.resolve(fields, layouts);

        assertThat(resolver.rowFieldsStartingAt(FormFieldUtils.fetchFormFieldKey(volume1)))
                .extracting(FormFieldLayoutResolver.KeyedField::field)
                .containsExactly(volume1, unit1);
        assertThat(resolver.rowFieldsStartingAt(FormFieldUtils.fetchFormFieldKey(volume2)))
                .extracting(FormFieldLayoutResolver.KeyedField::field)
                .containsExactly(volume2, unit2);
        assertThat(resolver.isSkippedRowMember(FormFieldUtils.fetchFormFieldKey(unit1))).isTrue();
        assertThat(resolver.isSkippedRowMember(FormFieldUtils.fetchFormFieldKey(unit2))).isTrue();
    }

    private static FormField field(String title, String label, Integer blockInstance) {
        return FormField.builder()
                .title(title)
                .label(label)
                .blockInstance(blockInstance)
                .build();
    }

    private static Map<String, FormField> fieldsOf(FormField... fields) {
        return java.util.stream.Stream.of(fields).collect(FormFieldUtils.formFieldMapCollector());
    }

    private static String keyOf(Map<String, FormField> fields, String label) {
        return FormFieldUtils.fetchFormFieldKey(
                fields.values().stream().filter(f -> f.label().equals(label)).findFirst().orElseThrow());
    }
}
