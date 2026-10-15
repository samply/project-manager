package de.samply.utils.directory;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;

import java.nio.file.Path;
import java.util.Map;

@Configuration
public class DirectoryConversionConfiguration {

    @Bean
    public static BeanFactoryPostProcessor directoryConversionServicePostProcessor() {
        return beanFactory -> {
            ConversionService conversionService = beanFactory.getConversionService();
            ConfigurableConversionService configurableConversionService =
                    conversionService instanceof ConfigurableConversionService existingConversionService
                            ? existingConversionService
                            : new ApplicationConversionService();

            StringToPathConverter stringToPathConverter = new StringToPathConverter();

            configurableConversionService.addConverter(stringToPathConverter);
            configurableConversionService.addConverter(new StringToExistingDirectoryConverter(stringToPathConverter));
            configurableConversionService.addConverter(new StringToEnsuredDirectoryConverter(stringToPathConverter));

            if (conversionService != configurableConversionService) {
                beanFactory.setConversionService(configurableConversionService);
            }
        };
    }

    @Bean
    @ConfigurationPropertiesBinding
    public StringToPathConverter pathConverter() {
        return new StringToPathConverter();
    }

    @Bean
    @ConfigurationPropertiesBinding
    public StringToExistingDirectoryConverter existingDirectoryConverter(StringToPathConverter pathConverter) {
        return new StringToExistingDirectoryConverter(pathConverter);
    }

    @Bean
    @ConfigurationPropertiesBinding
    public StringToEnsuredDirectoryConverter ensuredDirectoryConverter(StringToPathConverter pathConverter) {
        return new StringToEnsuredDirectoryConverter(pathConverter);
    }

    @Bean
    public CustomEditorConfigurer pathEditorConfigurer() {
        CustomEditorConfigurer configurer = new CustomEditorConfigurer();
        configurer.setCustomEditors(Map.of(Path.class, NormalizingPathEditor.class));
        return configurer;
    }

}
