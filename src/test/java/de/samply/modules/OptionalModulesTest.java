package de.samply.modules;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.annotations.ConditionalOnModuleTest;
import de.samply.project.ProjectType;
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
            .withUserConfiguration(OptionalModules.class, FeasibilityComponents.class, ResearchEnvironmentComponents.class,
                    BeamComponents.class);

    @Test
    void createsModuleComponentsWhenTheModuleIsEnabledOrUnset() {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=true", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).hasSingleBean(FeasibilityBean.class)
                        .doesNotHaveBean(DisabledFeasibilityBean.class)
                        .hasSingleBean(ResearchEnvironmentBean.class));
    }

    @Test
    void doesNotCreateModuleComponentsWhenTheModuleIsDisabled() {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=false", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).doesNotHaveBean(FeasibilityBean.class)
                        .hasSingleBean(DisabledFeasibilityBean.class)
                        .hasSingleBean(ResearchEnvironmentBean.class));
    }

    @Test
    void testModeCreatesTheTestBeansInsteadOfTheRealOnesAndNeedsNoBeam() {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=test", "ENABLE_EXPORTER=false",
                        "ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=false")
                .run(context -> assertThat(context).hasNotFailed()
                        .hasSingleBean(TestFeasibilityBean.class)
                        .hasSingleBean(FeasibilityMapperBean.class)
                        .doesNotHaveBean(FeasibilityBean.class)
                        .doesNotHaveBean(DisabledFeasibilityBean.class)
                        .doesNotHaveBean(BeamBean.class));
    }

    @Test
    void sharedBeanExistsInTheRealModeToo() {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=true", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).hasSingleBean(FeasibilityMapperBean.class)
                        .doesNotHaveBean(TestFeasibilityBean.class));
    }

    @Test
    void stopsTheStartOnAnInvalidValue() {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=yes", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause()
                        .hasMessageContaining("ENABLE_FEASIBILITY=yes is not valid; allowed: true, false, test"));
    }

    @Test
    void stopsTheStartOnTestModeOfAModuleWithoutOne() {
        contextRunner.withPropertyValues("ENABLE_DATASHIELD=test", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause()
                        .hasMessageContaining("ENABLE_DATASHIELD=test: module DATASHIELD has no test mode"));
    }

    @Test
    void logsTheTestMode(CapturedOutput output) {
        contextRunner.withPropertyValues("ENABLE_FEASIBILITY=test", "ENABLE_EXPORTER=false",
                        "ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=false", "ENABLE_EMAILS=false")
                .run(context -> {
                    context.getBean(OptionalModules.class).logEnabledModules();
                    assertThat(output).contains("Enabled optional modules: FEASIBILITY [test] (FeasibilityMapperBean, TestFeasibilityBean)");
                });
    }

    @Test
    void offersOnlyRequestTypesWhoseModulesAreEnabled() {
        contextRunner.withPropertyValues("ENABLE_DATASHIELD=false", "ENABLE_RESEARCH_ENVIRONMENT=false")
                .run(context -> {
                    OptionalModules optionalModules = context.getBean(OptionalModules.class);
                    assertThat(optionalModules.fetchAvailableProjectTypes())
                            .containsExactly(ProjectType.EXPORT, ProjectType.SAMPLES, ProjectType.SEQUENCING);
                    assertThat(optionalModules.describeUnavailable(ProjectType.DATASHIELD))
                            .isEqualTo("Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)");
                });
    }

    @Test
    void externalExecutionIsDisabledUnlessItsVariableIsTrue() {
        contextRunner.withPropertyValues(RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context.getBean(OptionalModules.class).isEnabled(OptionalModule.EXTERNAL_EXECUTION)).isFalse());
        contextRunner.withPropertyValues("ENABLE_EXTERNAL_EXECUTION=true", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context.getBean(OptionalModules.class).isEnabled(OptionalModule.EXTERNAL_EXECUTION)).isTrue());
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
    void stopsTheStartWhenTheResearchEnvironmentIsEnabledWithoutTheExporter() {
        contextRunner.withPropertyValues("ENABLE_EXPORTER=false", "ENABLE_DATASHIELD=false", RESEARCH_ENVIRONMENT_TEST_URL)
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause()
                        .hasMessageContaining("RESEARCH_ENVIRONMENT (ENABLE_RESEARCH_ENVIRONMENT) requires EXPORTER (ENABLE_EXPORTER)"));
    }

    @Test
    void implicitModuleIsEnabledByAModuleThatRequiresIt() {
        contextRunner.withPropertyValues("ENABLE_EXPORTER=false", "ENABLE_FEASIBILITY=true",
                        "ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=false")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(BeamBean.class));
    }

    @Test
    void implicitModuleIsDisabledWithoutAModuleThatRequiresIt() {
        contextRunner.withPropertyValues("ENABLE_EXPORTER=false", "ENABLE_FEASIBILITY=false",
                        "ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(BeamBean.class));
    }

    @Test
    void logsTheEnabledModulesWithTheirBeans(CapturedOutput output) {
        contextRunner.withPropertyValues("ENABLE_RESEARCH_ENVIRONMENT=false", "ENABLE_DATASHIELD=false", "ENABLE_EMAILS=false")
                .run(context -> {
                    context.getBean(OptionalModules.class).logEnabledModules();
                    assertThat(output).contains("Enabled optional modules: BEAM (BeamBean) [through EXPORTER, FEASIBILITY], "
                            + "EXPORTER, FEASIBILITY (FeasibilityBean, FeasibilityMapperBean)");
                });
    }

    static class FeasibilityBean {
    }

    static class DisabledFeasibilityBean {
    }

    static class TestFeasibilityBean {
    }

    static class FeasibilityMapperBean {
    }

    record ResearchEnvironmentBean(String url) {
    }

    static class BeamBean {
    }

    @Configuration(proxyBeanMethods = false)
    static class FeasibilityComponents {

        @Bean
        @ConditionalOnModule(OptionalModule.FEASIBILITY)
        FeasibilityBean feasibilityBean() {
            return new FeasibilityBean();
        }

        @Bean
        @ConditionalOnModuleDisabled(OptionalModule.FEASIBILITY)
        DisabledFeasibilityBean disabledFeasibilityBean() {
            return new DisabledFeasibilityBean();
        }

        @Bean
        @ConditionalOnModuleTest(OptionalModule.FEASIBILITY)
        TestFeasibilityBean testFeasibilityBean() {
            return new TestFeasibilityBean();
        }

        // Needed in both the real and the test mode
        @Bean
        @ConditionalOnModule(OptionalModule.FEASIBILITY)
        @ConditionalOnModuleTest(OptionalModule.FEASIBILITY)
        FeasibilityMapperBean feasibilityMapperBean() {
            return new FeasibilityMapperBean();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class ResearchEnvironmentComponents {

        @Bean
        @ConditionalOnModule(OptionalModule.RESEARCH_ENVIRONMENT)
        ResearchEnvironmentBean researchEnvironmentBean(
                @Value("${RESEARCH_ENVIRONMENT_TEST_URL}") String url) {
            return new ResearchEnvironmentBean(url);
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class BeamComponents {

        @Bean
        @ConditionalOnModule(OptionalModule.BEAM)
        BeamBean beamBean() {
            return new BeamBean();
        }

    }

}
