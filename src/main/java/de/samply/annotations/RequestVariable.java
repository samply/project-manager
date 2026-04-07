package de.samply.annotations;

import de.samply.app.ProjectManagerConst;
import de.samply.resolvers.RequestVariableAndParameterMethodArgumentResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to bind request parameters from either the request body or query parameters.
 * This allows flexible handling of request variables by attempting to extract values
 * from both the query parameters and the JSON body, providing an elegant fallback mechanism.
 *
 * <p>
 * The annotation can be used on controller method parameters, and the corresponding values
 * will be retrieved either from the query parameters or the JSON body (if the parameter is missing
 * in the query parameters). It simplifies code and ensures that the necessary parameters are
 * available regardless of where they are specified in the request.
 * </p>
 *
 * <h3>Example of JSON Body</h3>
 *
 * <pre>{@code
 * POST /create HTTP/1.1
 * Content-Type: application/json
 *
 * {
 *   "query": "SELECT * FROM projects WHERE status = 'active'",
 *   "queryFormat": "SQL",
 *   "label": "Active Projects",
 *   "description": "Projects that are currently active."
 * }
 * }</pre>
 *
 * <p>In the above example:</p>
 * <ul>
 *   <li>{@code query}: The SQL query string passed in the JSON body</li>
 *   <li>{@code queryFormat}: The format of the query (e.g., SQL)</li>
 *   <li>{@code label}: Optional label</li>
 *   <li>{@code description}: Optional description</li>
 * </ul>
 *
 * <h3>How it works</h3>
 * <ul>
 *   <li>The values for parameters annotated with {@code @RequestVariable} are first extracted
 *       from the JSON body if present.</li>
 *   <li>If the value is not found in the body, the resolver falls back to query parameters.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 *
 * <pre>{@code
 * {@literal @}PostMapping("/create")
 * public ResponseEntity<String> createProject(
 *     {@literal @}RequestVariable(name = "query", required = true) String query,
 *     {@literal @}RequestVariable(name = "queryFormat", required = true) String queryFormat,
 *     {@literal @}RequestVariable(name = "label", required = false) String label,
 *     {@literal @}RequestVariable(name = "description", required = false) String description
 * ) {
 *     return ResponseEntity.ok("ProjectCode created successfully!");
 * }
 * }</pre>
 *
 * <p>In this example:</p>
 * <ul>
 *   <li>{@code query} and {@code queryFormat} are required and will be extracted from either
 *       the JSON body or query parameters.</li>
 *   <li>{@code label} and {@code description} are optional.</li>
 * </ul>
 *
 * @see RequestVariableAndParameterMethodArgumentResolver
 */


@Target(ElementType.PARAMETER) // Applies to method parameters
@Retention(RetentionPolicy.RUNTIME) // Retain at runtime for reflection
public @interface RequestVariable {
    String name();

    boolean required() default true;

    boolean notEmpty() default false;

    String defaultValue() default ProjectManagerConst.NOT_SET;
}
