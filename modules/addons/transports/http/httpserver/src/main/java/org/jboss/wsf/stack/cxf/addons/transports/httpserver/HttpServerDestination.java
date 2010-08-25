/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.wsf.stack.cxf.addons.transports.httpserver;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http_jaxws_spi.HttpHandlerImpl;
import org.apache.cxf.transport.http_jaxws_spi.JAXWSHttpSpiDestination;
import org.jboss.ws.httpserver_httpspi.HttpExchangeDelegate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * HTTP destination to be used with the JDK6 httpserver; this extends the
 * basic JAXWSHttpSpiDestination with all the mechanisms for properly
 * handling destination and factory life-cycles.
 * 
 * @author alessio.soldano@jboss.com
 * @since 19-Aug-2010
 *
 */
@SuppressWarnings("restriction")
public class HttpServerDestination extends JAXWSHttpSpiDestination
{
   static final Logger LOG = LogUtils.getL7dLogger(HttpServerDestination.class);

   private static final long serialVersionUID = 1L;

   private HttpServerTransportFactory factory;
   private HttpServerEngineFactory serverEngineFactory;
   private HttpServerEngine engine;
   private URL url;

   public HttpServerDestination(Bus b, HttpServerTransportFactory factory, EndpointInfo ei) throws IOException
   {
      super(b, ei);
      this.factory = factory;
      this.serverEngineFactory = factory.getServerEngineFactory();
      this.url = new URL(ei.getAddress());
   }

   @Override
   protected Logger getLogger()
   {
      return LOG;
   }

   public void finalizeConfig() throws IOException
   {
      engine = serverEngineFactory.retrieveHttpServerEngine(url.getPort());
      if (engine == null)
      {
         engine = serverEngineFactory.createHttpServerEngine(url.getHost(), url.getPort(), url.getProtocol());
      }
      if (!url.getProtocol().equals(engine.getProtocol()))
      {
         throw new IllegalStateException("Port " + engine.getPort() + " is configured with wrong protocol \""
               + engine.getProtocol() + "\" for \"" + url + "\"");
      }
   }

   /**
    * Activate receipt of incoming messages.
    */
   protected void activate()
   {
      LOG.log(Level.FINE, "Activating receipt of incoming messages");
      String addr = endpointInfo.getAddress();
      try
      {
         new URL(addr);
      }
      catch (Exception e)
      {
         throw new Fault(e);
      }
      engine.addHandler(addr, new Handler(this));
   }

   /**
    * Deactivate receipt of incoming messages.
    */
   protected void deactivate()
   {
      LOG.log(Level.FINE, "Deactivating receipt of incoming messages");
      engine.removeHandler(endpointInfo.getAddress());
   }

   @Override
   public void shutdown()
   {
      factory.removeDestination(endpointInfo);
      super.shutdown();
   }

   class Handler extends HttpHandlerImpl implements HttpHandler
   {

      public Handler(JAXWSHttpSpiDestination destination)
      {
         super(destination);
      }

      @Override
      public void handle(HttpExchange ex) throws IOException
      {
         try
         {
            this.handle(new HttpExchangeDelegate(ex));
         }
         catch (Exception e)
         {
            LOG.throwing(Handler.class.getName(), "handle(com.sun.net.httpserver.HttpExchange ex)", e);
            if (e instanceof IOException)
            {
               throw (IOException) e;
            }
            else
            {
               throw new RuntimeException(e);
            }
         }
      }
   }

}