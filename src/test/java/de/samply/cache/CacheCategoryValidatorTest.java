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
        List<String> missingCategories = new ArrayList<>();

        for (Method method : ProjectManagerController.class.getDeclaredMethods()) {
            if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null
                    && AnnotatedElementUtils.findMergedAnnotation(method, CacheCategory.class) == null) {
                missingCategories.add(method.getName());
            }
        }

        CacheCategory assetsCategory = AnnotatedElementUtils.findMergedAnnotation(
                ProjectManagerWebConfig.class, CacheCategory.class);
        if (assetsCategory == null || assetsCategory.value() != CacheResource.BACKEND_ASSETS) {
            missingCategories.add(ProjectManagerWebConfig.class.getSimpleName() + " (/assets/**)");
        }

        assertThat(missingCategories)
                .as("Every endpoint and the /assets/** resource handler must have a cache category")
                .isEmpty();
    }
}
