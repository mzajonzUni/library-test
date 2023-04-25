package pl.zajonz.librarytest.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EntityNotFoundException.class, UsernameNotFoundException.class})
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ErrorMessage handleNotFoundException(EntityNotFoundException ex){
        return new ErrorMessage(ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorMessage handleEntityNotFoundException(IllegalArgumentException ex){
        return new ErrorMessage(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorMessage handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ValidationErrorMessage error = new ValidationErrorMessage();
        ex.getFieldErrors().forEach(fe -> error.addViolation(fe.getField(), fe.getDefaultMessage()));
        return error;
    }

}
