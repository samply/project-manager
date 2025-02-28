package de.samply.annotations;

import jakarta.validation.constraints.NotNull;

import java.lang.annotation.*;

@Repeatable(FrontendSiteModules.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FrontendSiteModule {
    @NotNull
    String site();

    @NotNull
    String module();

}
