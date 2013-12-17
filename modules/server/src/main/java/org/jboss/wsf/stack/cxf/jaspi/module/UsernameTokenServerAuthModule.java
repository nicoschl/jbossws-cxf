/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.wsf.stack.cxf.jaspi.module;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.xml.soap.SOAPMessage;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.ws.security.SecurityConstants;
import org.jboss.security.auth.container.modules.AbstractServerAuthModule;
import org.jboss.wsf.stack.cxf.security.authentication.JaspiSubjectCreatingInterceptor;

/** 
 * @author <a href="ema@redhat.com">Jim Ma</a>
 */
public class UsernameTokenServerAuthModule extends AbstractServerAuthModule
{
   private String securityDomainName = null;
   private Endpoint endpoint = null;
   private Bus bus = null;
   private InterceptorProvider ip = null;

   @SuppressWarnings("rawtypes")
   public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException
   {
      super.initialize(requestPolicy, responsePolicy, handler, options);
      endpoint = (Endpoint)options.get(Endpoint.class);
      if (endpoint == null && options.get(Bus.class) != null)
      {
         bus = (Bus)options.get(Bus.class);
         bus.setProperty(SecurityConstants.VALIDATE_TOKEN, false);
         ip = (InterceptorProvider)bus;
      }
      if (endpoint != null) {
         endpoint.put(SecurityConstants.VALIDATE_TOKEN, false);
         ip = (InterceptorProvider)endpoint;
      }
      if (ip != null)
      {
         JaspiSubjectCreatingInterceptor jaspiInterceptor = new JaspiSubjectCreatingInterceptor(securityDomainName);
         ip.getInInterceptors().add(jaspiInterceptor);
      }

   }

   public UsernameTokenServerAuthModule()
   {
      supportedTypes.add(Object.class);
      supportedTypes.add(SOAPMessage.class);
   }

   public UsernameTokenServerAuthModule(String lmshName)
   {
      supportedTypes.add(Object.class);
      this.supportedTypes.add(SOAPMessage.class);
      securityDomainName = lmshName;
   }

   @Override
   public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException
   {
      return AuthStatus.SUCCESS;
   }

   public AuthStatus secureResponse(MessageInfo arg0, Subject arg1) throws AuthException
   {
      throw new UnsupportedOperationException();
   }

   protected String getSecurityDomainName()
   {
      if (this.securityDomainName != null)
         return securityDomainName;

      // Check if it is passed in the options
      String domainName = (String)options.get("javax.security.auth.login.LoginContext");
      if (domainName == null)
      {
         domainName = getClass().getName();
      }
      return domainName;
   }

   @Override
   protected boolean validate(Subject clientSubject, MessageInfo messageInfo) throws AuthException
   {
      return true;
   }

}