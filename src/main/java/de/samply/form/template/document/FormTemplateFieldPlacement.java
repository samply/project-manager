package de.samply.form.template.document;

import de.samply.frontend.dto.FormField;
import de.samply.utils.FormFieldUtils;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orders the fields of a form template the way the frontend Summary orders them
 * (ProjectView.vue, sortConfiguredProjectFields):
 * <ol>
 *   <li>the header block: project fields without a (printed) section, in
 *   configuration order;</li>
 *   <li>then one section per printed form, in the given section order. Inside
 *   a section, project fields placed there without an order come first, in
 *   configuration order (like the frontend's native fixed fields); then the
 *   form's dynamic fields and the project fields that follow a FIXED entry's
 *   order, merged by order. A block is one ordering unit placed at its lowest
 *   order, its fields sorted by instance, then order, so each instance stays
 *   together.</li>
 * </ol>
 */
public final class FormTemplateFieldPlacement {

    /** A field, its fields-map key and its section; a null section is the header block. */
    public record PlacedField(String key, FormField field, String section) {
    }

    /**
     * A project field with its resolved section (null: header block) and, if it
     * takes part in the section's order, its order (null: section start).
     */
    public record ProjectFieldPlacement(FormField field, String section, Integer order) {
    }

    private record Candidate(FormField field, int order, int sourceIndex) {
    }

    private static final class Unit {
        private int order;
        private int sourceIndex;
        private final List<Candidate> candidates = new ArrayList<>();

        private Unit(int order, int sourceIndex) {
            this.order = order;
            this.sourceIndex = sourceIndex;
        }
    }

    private FormTemplateFieldPlacement() {
    }

    public static List<PlacedField> place(
            @NotNull List<String> sectionOrder,
            @NotNull List<ProjectFieldPlacement> projectFields,
            @NotNull List<FormField> dynamicFields) {
        Set<String> sections = new HashSet<>(sectionOrder);
        List<PlacedField> result = new ArrayList<>();

        projectFields.stream()
                .filter(placement -> placement.section() == null || !sections.contains(placement.section()))
                .forEach(placement -> result.add(placed(placement.field(), null)));

        for (String section : sectionOrder) {
            projectFields.stream()
                    .filter(placement -> section.equals(placement.section()) && placement.order() == null)
                    .forEach(placement -> result.add(placed(placement.field(), section)));

            // Project fields first, like the frontend lists configured fixed
            // fields before dynamic ones; only matters for equal orders.
            List<Candidate> candidates = new ArrayList<>();
            projectFields.stream()
                    .filter(placement -> section.equals(placement.section()) && placement.order() != null)
                    .forEach(placement -> candidates.add(
                            new Candidate(placement.field(), placement.order(), candidates.size())));
            dynamicFields.stream()
                    .filter(field -> section.equals(field.title()))
                    .forEach(field -> candidates.add(
                            new Candidate(field, orderOf(field.order()), candidates.size())));

            orderLikeTheSummary(candidates).forEach(field -> result.add(placed(field, section)));
        }
        return result;
    }

    private static List<FormField> orderLikeTheSummary(List<Candidate> candidates) {
        List<Unit> units = new ArrayList<>();
        Map<String, Unit> blockUnits = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            String block = candidate.field().block();
            if (block == null) {
                Unit unit = new Unit(candidate.order(), candidate.sourceIndex());
                unit.candidates.add(candidate);
                units.add(unit);
                continue;
            }
            Unit unit = blockUnits.computeIfAbsent(block, _ -> {
                Unit blockUnit = new Unit(candidate.order(), candidate.sourceIndex());
                units.add(blockUnit);
                return blockUnit;
            });
            unit.order = Math.min(unit.order, candidate.order());
            unit.sourceIndex = Math.min(unit.sourceIndex, candidate.sourceIndex());
            unit.candidates.add(candidate);
        }

        units.sort(Comparator.<Unit>comparingInt(unit -> unit.order).thenComparingInt(unit -> unit.sourceIndex));
        Comparator<Candidate> withinUnit = Comparator
                .<Candidate>comparingInt(candidate -> orderOf(candidate.field().blockInstance()))
                .thenComparingInt(Candidate::order)
                .thenComparingInt(Candidate::sourceIndex);
        return units.stream()
                .flatMap(unit -> unit.candidates.stream().sorted(withinUnit))
                .map(Candidate::field)
                .toList();
    }

    // A missing order (or block instance) sorts last, as in the frontend.
    private static int orderOf(Integer value) {
        return value != null ? value : Integer.MAX_VALUE;
    }

    private static PlacedField placed(FormField field, String section) {
        return new PlacedField(FormFieldUtils.fetchFormFieldKey(field), field, section);
    }
}
