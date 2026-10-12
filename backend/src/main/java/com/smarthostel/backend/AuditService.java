package com.smarthostel.backend;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditEventRepository events;
  public AuditService(AuditEventRepository events){this.events=events;}
  public void record(String action,String type,Object id){
    Authentication auth=SecurityContextHolder.getContext().getAuthentication();
    String actor=auth==null||!auth.isAuthenticated()||"anonymousUser".equals(auth.getName())?"system":auth.getName();
    events.save(new AuditEvent(actor,action,type,id==null?null:String.valueOf(id)));
  }
}
