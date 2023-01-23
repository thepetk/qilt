package org.acme;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.config.Priorities;
import io.smallrye.context.api.ManagedExecutorConfig;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

import com.sun.mail.imap.IdleManager;


@ApplicationScoped
public class Qilt {

    @ConfigProperty(name = "qilt.imap.password")
    String imapPassword;
    
    @ConfigProperty(name = "qilt.imap.host",defaultValue = "imap.gmail.com")
    String imapHost;


    @ConfigProperty(name = "qilt.imap.user")
    String imapUser;

    final String lineSeparator = System.getProperty("line.separator");

    @Inject Logger LOGGER;

    @ManagedExecutorConfig //(maxAsync = 1)
    @Inject ManagedExecutor executor;

    void onStart(@Observes /*@Priority(Priorities.APPLICATION)*/ StartupEvent ev) throws IOException //throws InterruptedException, MessagingException, IOException {  
    {
        if (ev==null) throw new IOException("blah");
        //   System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");             
        LOGGER.info("The application is starting...");
        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(props, null);
       // org.jboss.logging.LoggerStream x;

       


        session.setDebugOut(new PrintStream(new ByteArrayOutputStream() {

            
            @Override
            public void flush() throws IOException {
                String record;
        synchronized (this) {
            super.flush();
            record = this.toString().trim();
            super.reset();

            if (record.length() == 0 || record.equals(lineSeparator)) {
                // avoid empty records
                return;
            }

            LOGGER.info(record);
        }
            }
        },true));
        //session.setDebug(true);
        ExecutorService es = executor;
        Properties sprops = session.getProperties();
        sprops.put("mail.event.scope", "session"); // or "application"
        sprops.put("mail.event.executor", es);
        sprops.put("mail.imaps.usesocketchannels", true);

        //https://stackoverflow.com/questions/2538481/javamail-performance
        sprops.put("mail.imaps.fetchsize", "3000000");

        Store store;
        try {
            store = session.getStore("imaps");
        
        store.connect(imapHost, imapUser, imapPassword);

        Folder f = store.getFolder("INBOX");
        f.open(Folder.READ_ONLY);
       // System.out.println(f.getName() + ":" + f.getUnreadMessageCount());
        //filterMessages(f.getMessages()).forEach(m -> processMessage(m));

        boolean watch = true;
        if (watch) {
            IdleManager idleManager = new IdleManager(session, es);
            f.addMessageCountListener(new MessageCountAdapter() {
                public void messagesAdded(MessageCountEvent ev) {
                    Folder folder = (Folder)ev.getSource();
                    Message[] msgs = ev.getMessages();
                  //  System.out.println("Folder: " + folder +
                    //    " got " + msgs.length + " new messages");
                    //filterMessages(msgs).forEach(m -> processMessage(m));
                    
                    try {
                        // keep watching for new messages
                      //  System.out.println("Waiting for messages...");
                        idleManager.watch(folder);
                    } catch (MessagingException mex) {
                        // ignore
                    }
                }
            });
        //    System.out.println("Waiting for messages...");
            idleManager.watch(f);
            
            //es.awaitTermination(1000, TimeUnit.DAYS);
        }
    } catch (NoSuchProviderException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (MessagingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
        
       // return f.getUnreadMessageCount();
    }

}