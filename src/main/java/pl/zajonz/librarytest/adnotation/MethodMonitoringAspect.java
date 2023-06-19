package pl.zajonz.librarytest.adnotation;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.repository.UserRepository;
import pl.zajonz.librarytest.service.MessageSender;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class MethodMonitoringAspect {

    private final UserRepository userRepository;
    private final MessageSender messageSender;

    @Around("@annotation(MonitorMethod)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalDateTime startMethodDateTime = LocalDateTime.now();

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;

        User user = getUser();
        messageSender.sendPerformanceInfo(user, executionTime,
                joinPoint.getSignature().toShortString(), startMethodDateTime);

        return result;
    }

    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User unknownUser = User.builder()
                .id(0)
                .email("unknown")
                .build();
        return userRepository.findByUsername(authentication.getName()).orElse(unknownUser);
    }
}
