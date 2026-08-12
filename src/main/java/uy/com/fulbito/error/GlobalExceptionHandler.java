package uy.com.fulbito.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.OffsetDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return response(ex.getStatus(), ex.getMessage(), req, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String,String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Hay datos invalidos en la solicitud", req, fields);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String details = Optional.ofNullable(ex.getMostSpecificCause()).map(Throwable::getMessage).orElse("");
        String message = details.contains("ex_court_occupancies_no_overlap")
            ? "La cancha ya esta ocupada total o parcialmente en ese horario"
            : details.contains("ex_opening_hours_no_overlap")
                ? "El horario se superpone con otro horario del complejo"
                : "La operacion viola una regla de integridad de la base de datos";
        return response(HttpStatus.CONFLICT, message, req, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return response(HttpStatus.BAD_REQUEST, "El JSON contiene un valor o formato invalido", req, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        return response(HttpStatus.NOT_FOUND, "Ruta no encontrada", req, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        // No se devuelve ex.getMessage(): podria revelar SQL, rutas o datos sensibles.
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno", req, Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest req, Map<String,String> fields) {
        return ResponseEntity.status(status).body(new ApiError(OffsetDateTime.now(), status.value(),
            status.getReasonPhrase(), message, req.getRequestURI(), fields));
    }
}
