package org.acme;

import java.io.IOException;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;

@Path("/hello")
public class GreetingResource {

    @Inject Logger logger;
    @Inject ManagedExecutor executor;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws InterruptedException, MessagingException, IOException {
     //   logger.info("Trying to create acb");
      //  AppLifecycleBean acb = new AppLifecycleBean(logger, executor);
     //   logger.info("Created acb " + acb);
        return "Hello from RESTEasy Reactive";
    }

    
}