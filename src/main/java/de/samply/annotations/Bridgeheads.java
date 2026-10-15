package de.samply.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Several bridgeheads of the request, e.g. {@code List<ProjectBridgehead>} from comma-separated ids. Unlike
 * {@link Bridgehead}, a plain marker: it does not make an action require a bridgehead, the constraint aspects do not
 * check against it, and the project is not derived from it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Bridgeheads {
}
