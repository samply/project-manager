package de.samply.utils.directory;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;

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

}
