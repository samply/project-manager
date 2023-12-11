package de.samply.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogUtils {

    public void logError(Throwable throwable){
        log.error(ExceptionUtils.getStackTrace(throwable));
    }

}
