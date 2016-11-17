package com.zorroa.archivist.web.api;

import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.DuplicateElementException;
import com.zorroa.sdk.client.exception.MissingElementException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

/**
 * Created by chambers on 11/7/16.
 */

@Aspect
@Component
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Pointcut("execution(public * *(..))")
    public void execute() {}

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {}

    @Around("restController() && execute()")
    public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (ArchivistException e) {
            throw e;
        } catch (EmptyResultDataAccessException e) {
            logStackTrace(e);
            throw new MissingElementException(e.getMessage());
        }
        catch (DataIntegrityViolationException e) {
            logStackTrace(e);
            throw new DuplicateElementException(e.getMessage());
        }
        catch (Exception e) {
            throw new ArchivistException(e);
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

