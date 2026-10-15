package de.samply.cache;

import de.samply.app.ProjectManagerController;
import de.samply.app.ProjectManagerWebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Developer/CI validation for cache categories on application handlers. */
class CacheCategoryValidatorTest {

    @Test
    void everyControllerEndpointHasCacheCategory() {
        List<String> violations = new ArrayList<>();

        for (Method method : ProjectManagerController.class.getDeclaredMethods()) {
            if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null
                    && AnnotatedElementUtils.findMergedAnnotation(method, CacheCategory.class) == null) {
                violations.add(ProjectManagerController.class.getSimpleName() + "#" + method.getName()
                        + ": missing @CacheCategory");
            }
        }

        CacheCategory assetsCategory = AnnotatedElementUtils.findMergedAnnotation(
                ProjectManagerWebConfig.class, CacheCategory.class);
        if (assetsCategory == null || assetsCategory.value() != CacheResource.BACKEND_ASSETS) {
            violations.add(ProjectManagerWebConfig.class.getSimpleName()
                    + " (/assets/**): expected @CacheCategory(CacheResource.BACKEND_ASSETS)");
        }

        assertThat(violations)
                .withFailMessage("""
                        Cache category validation failed:
                        %s

                        Every endpoint and the /assets/** resource handler must declare a cache category. The category
                        determines the Cache-Control policy returned to clients; omitting or choosing the wrong category
                        can make sensitive or stale responses cacheable for an inappropriate amount of time.

                        Add @CacheCategory with the most appropriate CacheResource to each listed handler. For example:

                            @CacheCategory(CacheResource.PROJECT_DETAIL)
                            @GetMapping("/projects/{projectCode}")
                            public ResponseEntity<?> fetchProject(...) { ... }

                        See CacheResource for the available categories. The /assets/** handler must use BACKEND_ASSETS.
                        """, String.join(System.lineSeparator(), violations))
                .isEmpty();
    }
}
