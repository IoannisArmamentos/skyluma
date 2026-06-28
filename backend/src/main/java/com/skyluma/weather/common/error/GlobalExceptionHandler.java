package com.skyluma.weather.common.error;

import com.skyluma.weather.openmeteo.client.OpenMeteoClientException;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolation(ConstraintViolationException exception) {
        List<String> messages = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getMessage())
                .toList();

        return badRequest(messages);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMissingRequestParameter(MissingServletRequestParameterException exception) {
        return badRequest(List.of("Missing required request parameter: " + exception.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest(List.of("Invalid value for request parameter: " + exception.getName()));
    }

    @ExceptionHandler(OpenWeatherClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponse handleOpenWeatherClientException(OpenWeatherClientException exception) {
        return badGateway(List.of(exception.getMessage()));
    }

    @ExceptionHandler(OpenMeteoClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponse handleOpenMeteoClientException(OpenMeteoClientException exception) {
        return badGateway(List.of(exception.getMessage()));
    }

    private ApiErrorResponse badRequest(List<String> messages) {
        return new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                messages
        );
    }

    private ApiErrorResponse badGateway(List<String> messages) {
        return new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                messages
        );
    }
}