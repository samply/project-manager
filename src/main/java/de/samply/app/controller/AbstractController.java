package de.samply.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

/**
 * The response helpers shared by the controllers (see ProjectManagerControllers).
 * <p>
 * The controllers intentionally return raw ResponseEntity to avoid excessive generic
 * duplication across many endpoints. Parameters such as bridgehead and project are not
 * always used directly in the method body but are required for validation and
 * authorization in the AOP layer.
 */
@SuppressWarnings({"rawtypes"})
public abstract class AbstractController {

    protected final ObjectMapper objectMapper = new ObjectMapper().enable(
                    SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    protected ResponseEntity convertToResponseEntity(RunnableWithException runnable) {
        try {
            runnable.run();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return createInternalServerError(e);
        }
    }

    protected <T> ResponseEntity convertOptionalToResponseEntity(
            SupplierWithException<Optional<T>> supplier) {
        try {
            Optional<T> result = supplier.get();
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok(objectMapper.writeValueAsString(result.get()));
            }
        } catch (Exception e) {
            return createInternalServerError(e);
        }
    }

    protected <T> ResponseEntity convertToResponseEntity(SupplierWithException<T> supplier) {
        try {
            T result = supplier.get();
            if (result == null) {
                return ResponseEntity.notFound().build();
            } else if (result instanceof ResponseEntity) {
                return (ResponseEntity) result;
            } else {
                return ResponseEntity.ok(objectMapper.writeValueAsString(result));
            }
        } catch (Exception e) {
            return createInternalServerError(e);
        }
    }

    protected ResponseEntity createInternalServerError(Exception e) {
        return ResponseEntity.internalServerError().body(ExceptionUtils.getStackTrace(e));
    }

    protected interface RunnableWithException {

        void run() throws Exception;
    }

    protected interface SupplierWithException<T> {

        T get() throws Exception;
    }

}
