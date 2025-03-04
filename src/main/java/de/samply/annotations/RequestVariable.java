package de.samply.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to bind request parameters from either the request body or query parameters.
 * This allows flexible handling of request variables by attempting to extract values
 * from both the query parameters and the JSON body, providing an elegant fallback mechanism.
 *
 * <p>The annotation can be used on controller method parameters, and the corresponding values
 * will be retrieved either from the query parameters or the JSON body (if the parameter is missing
 * in the query parameters). It simplifies code and ensures that necessary parameters are available
 * regardless of where they are specified in the request.</p>
 *
 * <h3>Example of JSON Body:</h3>
 *
 * When using `@RequestVariable` on parameters, here's an example of how the request body might look:
 *
 * <pre>
 * POST /create HTTP/1.1
 * Content-Type: application/json
 *
 * {
 *     "query": "SELECT * FROM projects WHERE status = 'active'",
 *     "queryFormat": "SQL",
 *     "label": "Active Projects",
 *     "description": "Projects that are currently active."
 * }
 * </pre>
 *
 * In the above example:
 * - `query`: The SQL query string is passed as a parameter in the JSON body.
 * - `queryFormat`: The format of the query (e.g., SQL) is also passed in the JSON body.
 * - `label`: An optional field for the label, passed in the JSON body.
 * - `description`: An optional field for a description, passed in the JSON body.
 *
 * <h3>How it works:</h3>
 * - The values for the parameters annotated with `@RequestVariable` will be extracted from the
 *   JSON body if available. If not present in the body, the annotation will fall back to
 *   extracting the value from the query parameters.
 *
 * <h3>Usage Example:</h3>
 *
 * <pre>
 * @PostMapping("/create")
 * public ResponseEntity<String> createProject(
 *     @RequestVariable(name = "query", required = true) String query,
 *     @RequestVariable(name = "queryFormat", required = true) String queryFormat,
 *     @RequestVariable(name = "label", required = false) String label,
 *     @RequestVariable(name = "description", required = false) String description
 * ) {
 *     // Handle the request
 *     return ResponseEntity.ok("Project created successfully!");
 * }
 * </pre>
 *
 * In this example:
 * - `query` and `queryFormat` are required, and will be extracted from either the JSON body
 *   or query parameters.
 * - `label` and `description` are optional and can be specified in the JSON body or query
 *   parameters.
 *
 * @see de.samply.resolvers.RequestVariableMethodArgumentResolver
 */
@Target(ElementType.PARAMETER) // Applies to method parameters
@Retention(RetentionPolicy.RUNTIME) // Retain at runtime for reflection
public @interface RequestVariable {
    String name();
    boolean required() default true;
    boolean notEmpty() default false;
}
