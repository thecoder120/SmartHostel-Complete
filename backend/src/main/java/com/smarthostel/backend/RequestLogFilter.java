package com.smarthostel.backend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLogFilter extends OncePerRequestFilter {
  private static final Logger log=LoggerFactory.getLogger(RequestLogFilter.class);
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
    String id=UUID.randomUUID().toString();long started=System.nanoTime();MDC.put("requestId",id);response.setHeader("X-Request-Id",id);
    try{chain.doFilter(request,response);}finally{
      log.info("request method={} path={} status={} durationMs={}",request.getMethod(),request.getRequestURI(),response.getStatus(),(System.nanoTime()-started)/1_000_000);
      MDC.remove("requestId");
    }
  }
}
