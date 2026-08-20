package uy.com.fulbito.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.OffsetDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
            : details.contains("ck_venue_images_max_8")
                ? "El complejo ya alcanzo el maximo de 8 imagenes"
                : details.contains("ck_court_images_max_5")
                    ? "La cancha ya alcanzo el maximo de 5 imagenes"
                    : details.contains("uq_venue_images_storage_key")
                        || details.contains("uq_court_images_storage_key")
                        ? "La imagen ya fue registrada"
            : details.contains("ex_opening_hours_no_overlap")
                ? "El horario se superpone con otro horario del complejo"
                : details.contains("ck_opening_hours_whole_minutes")
                    ? "Los horarios deben comenzar y terminar en minutos exactos"
                    : details.contains("ck_opening_hours_order")
                        ? "La hora de cierre debe ser posterior a la apertura"
                        : details.contains("ck_opening_hours_valid_range")
                            ? "La hora de cierre debe ser posterior a la apertura en el mismo dia"
                        : details.contains("ck_opening_hours_day")
                            ? "El dia seleccionado no es valido"
                : "La operacion viola una regla de integridad de la base de datos";
        return response(HttpStatus.CONFLICT, message, req, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return response(HttpStatus.BAD_REQUEST, "El JSON contiene un valor o formato invalido", req, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return response(HttpStatus.BAD_REQUEST, "El parametro '" + ex.getName() + "' tiene un formato invalido", req,
            Map.of(ex.getName(), "Formato invalido"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        return response(HttpStatus.NOT_FOUND, "Ruta no encontrada", req, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        // No se devuelve ex.getMessage(): podria revelar SQL, rutas o datos sensibles.
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno", req, Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest req, Map<String,String> fields) {
        return ResponseEntity.status(status).body(new ApiError(OffsetDateTime.now(), status.value(),
            status.getReasonPhrase(), message, req.getRequestURI(), fields));
    }
}
