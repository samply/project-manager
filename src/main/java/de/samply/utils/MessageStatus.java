package de.samply.utils;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public record MessageStatus(String message, HttpStatus status) {

    public static MessageStatus newInstance(@NotNull Throwable throwable, @NotNull String defaultErrorMessage) {
        String message = defaultErrorMessage;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException responseException = (WebClientResponseException) throwable;
            String responseBody = responseException.getResponseBodyAsString();
            if (responseBody != null && StringUtils.hasText(responseBody)) {
                message += " (" + responseBody + ")";
            }
            try {
                status = (HttpStatus) responseException.getStatusCode();
            } catch (IllegalArgumentException e) {
            }
        }
        return new MessageStatus(message, status);
    }

}
