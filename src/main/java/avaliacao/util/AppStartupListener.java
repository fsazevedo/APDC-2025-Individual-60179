package avaliacao.util;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.datastore.*;
import java.util.logging.Logger;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.annotation.WebListener;
@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartupListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
        Key userKey = datastore.newKeyFactory().setKind("User").newKey("root");

        Entity user = datastore.get(userKey);
        if (user == null) {
        	Entity newUser = Entity.newBuilder(userKey)
        		    .set("user_name", "root")
        		    .set("user_email", "admin@root.pt")
        		    .set("user_role", "ADMIN")
        		    .set("profile_type", "PRIVATE")
        		    .set("user_account_state", "ACTIVE")
        		    .set("user_creation_time", System.currentTimeMillis())
        		    .set("user_pwd", DigestUtils.sha512Hex("admin123"))
        		    .build();

            datastore.put(newUser);
            LOG.info("Root ADMIN user created successfully.");
        } else {
            LOG.info("Root ADMIN user already exists.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
