package de.samply.modules;

import de.samply.annotations.ModuleComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class OptionalModulesTest {

    // Configuration of the test research environment bean: required (no default) when the module is enabled
    private static final String RESEARCH_ENVIRONMENT_TEST_URL = "RESEARCH_ENVIRONMENT_TEST_URL=http://coder";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OptionalModules.class, FeasibilityComponents.class, ResearchEnvironmentComponents.class);

    @Test
    void createsModuleComponentsWhenTheModuleIsEnabledOrUnset() {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=true", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).hasSingleBean(FeasibilityBean.class)
                        .hasSingleBean(ResearchEnvironmentBean.class));
    }

    @Test
    void doesNotCreateModuleComponentsWhenTheModuleIsDisabled() {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=false", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).doesNotHaveBean(FeasibilityBean.class)
                        .hasSingleBean(ResearchEnvironmentBean.class));
    }

    @Test
    void disabledModuleNeedsNoConfiguration() {
        contextRunner.withPropertyValues("ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(ResearchEnvironmentBean.class));
    }

    @Test
    void stopsTheStartWhenARequiredModuleIsDisabled() {
        contextRunner.withPropertyValues("ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=true")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause()
                        .hasMessageContaining("DATASHIELD (ENABLE_DATASHIELD) requires RESEARCH_ENVIRONMENT (ENABLE_RESEARCH_ENVIRONMENT)"));
    }

    @Test
    void logsTheEnabledModulesWithTheirBeans(CapturedOutput output) {
        contextRunner.withPropertyValues("ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=false", "ENABLE_EMAILS=false")
                .run(context -> {
                    context.getBean(OptionalModules.class).logEnabledModules();
                    assertThat(output).contains("Enabled optional modules: EXPORTER, FEASIBILITY (FeasibilityBean)");
                });
    }

    static class FeasibilityBean {
    }

    record ResearchEnvironmentBean(String url) {
    }

    @Configuration(proxyBeanMethods = false)
    static class FeasibilityComponents {

        @Bean
        @ModuleComponent(OptionalModule.FEASIBILITY)
        FeasibilityBean feasibilityBean() {
            return new FeasibilityBean();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class ResearchEnvironmentComponents {

        @Bean
        @ModuleComponent(OptionalModule.RESEARCH_ENVIRONMENT)
        ResearchEnvironmentBean researchEnvironmentBean(
                @Value("${RESEARCH_ENVIRONMENT_TEST_URL}") String url) {
            return new ResearchEnvironmentBean(url);
        }

    }

}
