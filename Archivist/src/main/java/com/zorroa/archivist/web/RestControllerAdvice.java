package com.zorroa.archivist.web;

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
            throw new NullResultException(e);
        }
        catch (DataIntegrityViolationException e) {
            throw new DuplicateException(e);
        }
    }
}
