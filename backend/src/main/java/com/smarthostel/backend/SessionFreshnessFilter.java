package com.smarthostel.backend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

/** Invalidates sessions immediately after a password or role change. */
public class SessionFreshnessFilter extends OncePerRequestFilter {
  private final UserAccountRepository accounts;
  public SessionFreshnessFilter(UserAccountRepository accounts){this.accounts=accounts;}
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
    Authentication auth=SecurityContextHolder.getContext().getAuthentication();
    if(auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof UserDetails principal){
      UserAccount current=accounts.findByUsernameIgnoreCase(auth.getName()).orElse(null);
      boolean missingAccount=current==null;
      boolean passwordChanged=!missingAccount&&!current.getPasswordHash().equals(principal.getPassword());
      boolean roleChanged=!missingAccount&&!auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+current.getRole()));
      if(missingAccount||passwordChanged||roleChanged){
        SecurityContextHolder.clearContext();if(request.getSession(false)!=null)request.getSession(false).invalidate();response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Session expired; sign in again");return;
      }
      String path=request.getRequestURI();
      boolean passwordFlow=path.equals("/api/auth/password")||path.equals("/api/auth/me")||path.equals("/api/auth/csrf")||path.equals("/api/auth/config")||path.equals("/api/auth/logout");
      if(current.isMustChangePassword()&&!passwordFlow){response.setStatus(428);response.setContentType("application/problem+json");response.getWriter().write("{\"detail\":\"Change your temporary password to continue\"}");return;}
    }
    chain.doFilter(request,response);
  }
}
