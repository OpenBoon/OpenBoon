package com.zorroa.archivist.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by chambers on 4/14/17.
 */
public class CorsCredentialsFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory.getLogger(CorsCredentialsFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse rsp = (HttpServletResponse) servletResponse;

        // UNCOMMENT TO DEBUG THIS SHIT
        /*
        Enumeration<String> e = req.getHeaderNames();
        logger.info("{}------------------------------", req.getMethod());
        while (e.hasMoreElements()) {
            String name =e.nextElement();
            logger.info("{}={}", name, req.getHeader(name));
        }
        logger.info("-------------------------------");
        */
        
        String auth = req.getHeader("authorization");
        if (auth != null) {
            rsp.addHeader("Access-Control-Allow-Credentials", "true");
            rsp.addHeader("Access-Control-Allow-Origin", req.getHeader("origin"));
        }
        else {
            rsp.addHeader("Access-Control-Expose-Headers", "Content-Encoding, content-range, content-length, accept-ranges");
            rsp.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT, DELETE");
            rsp.addHeader("Access-Control-Allow-Headers", "authorization, content-type, x-requested-with");
            rsp.addHeader("Access-Control-Allow-Credentials", "true");
            rsp.addHeader("Access-Control-Allow-Origin", req.getHeader("origin"));
            if (req.getMethod().equals("OPTIONS")) {
                rsp.addHeader("Access-Control-Max-Age", "3600");
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
