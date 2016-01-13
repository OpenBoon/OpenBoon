package com.zorroa.archivist.web;

import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.web.exceptions.DuplicateException;
import com.zorroa.archivist.web.exceptions.NullResultException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

/**
 * Intercepts HTTP requests to the @RestController classes and converts all the different exceptions
 * Java can throw to a few basic ones that can be converted into equivalent client side exceptions.
 */
@Aspect
public class RestControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(RestControllerAdvice.class);

    @Pointcut("execution(public * *(..))")
    public void execute() {}

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {}

    @Around("restController() && execute()")
    public Object intercept(ProceedingJoinPoint pjp) throws Throwable {

        try {
            return pjp.proceed();
        } catch (EmptyResultDataAccessException e) {
            logStackTrace(e);
            throw new NullResultException(e.getMessage());
        }
        catch (DataIntegrityViolationException e) {
            logStackTrace(e);
            throw new DuplicateException(e.getMessage());
        }
    }

    private void logStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("REST error:");
        sb.append(e.getMessage());
        sb.append("\n");

        if (e.getCause() != null) {
            sb.append("Cause:");
            sb.append(e.getCause().getMessage());
            sb.append("\n");
        }

        StackTraceElement[] elements = e.getStackTrace();
        for (int i=0; i<10; i++) {
            sb.append(elements[i].toString());
            sb.append("\n");
        }

        sb.append("User: ");
        sb.append(SecurityUtils.getUsername());
        sb.append("\n");
        logger.warn(sb.toString());
    }
}
