package com.example.sis3.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void auditSuccess(JoinPoint joinPoint, Audited audited, Object result) {
        log.info("[AUDIT SUCCESS] action={} method={} args={} timestamp={}",
                audited.action(),
                joinPoint.getSignature().toShortString(),
                Arrays.toString(joinPoint.getArgs()),
                Instant.now());
    }

    @AfterThrowing(pointcut = "@annotation(audited)", throwing = "ex")
    public void auditFailure(JoinPoint joinPoint, Audited audited, Throwable ex) {
        log.warn("[AUDIT FAILURE] action={} method={} error={} timestamp={}",
                audited.action(),
                joinPoint.getSignature().toShortString(),
                ex.getMessage(),
                Instant.now());
    }
}