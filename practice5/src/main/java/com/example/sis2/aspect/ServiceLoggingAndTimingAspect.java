package com.example.sis2.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAndTimingAspect {

    @Pointcut("execution(* com.example.sis2.service..*(..))")
    public void serviceLayerMethods() {
    }

    @Around("serviceLayerMethods()")
    public Object logAndMeasureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info(">>> [AOP ENTER] {}.{}() with arguments: {}", className, methodName, Arrays.toString(args));

        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            log.info("<<< [AOP EXIT] {}.{}() completed in {} ms | result: {}",
                    className, methodName, durationMs, summarizeResult(result));
            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("xxx [AOP EXCEPTION] {}.{}() failed after {} ms with {}: {}",
                    className, methodName, durationMs, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private String summarizeResult(Object result) {
        if (result == null) {
            return "void/null";
        }
        String str = result.toString();
        return str.length() > 150 ? str.substring(0, 147) + "..." : str;
    }
}
