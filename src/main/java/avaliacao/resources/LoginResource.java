package avaliacao.resources;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import avaliacao.util.AuthToken;
import avaliacao.util.LoginData;

@Path("/login")
public class LoginResource {
	
	private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";
	private static final String MESSAGE_NEXT_PARAMETER_INVALID = "Request parameter 'next' must be greater or equal to 0.";

	private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";
	private static final String LOG_MESSAGE_UNKNOW_USER = "Failed login attempt for username: ";


	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	private final Gson g = new Gson();
	
	@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data,
	                        @Context HttpServletRequest request,
	                        @Context HttpHeaders headers) {

	    Key userKey = userKeyFactory.newKey(data.username);

	    Entity user = datastore.get(userKey);
	    if (user != null) {
	        String hashedPWD = user.getString("user_pwd");
	        if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
	            user = Entity.newBuilder(user)
	                    .set("user_login_time", Timestamp.now())
	                    .build();
	            datastore.update(user);

	            LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.username);

	            // Obter o papel do utilizador
	            String role = user.contains("user_role") ? user.getString("user_role") : "USER";

	            // Criar o token de autenticação completo
	            AuthToken token = new AuthToken(data.username, role);

	            return Response.ok(g.toJson(token)).build();
	        } else {
	            LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.username);
	            return Response.status(Status.FORBIDDEN)
	                    .entity(MESSAGE_INVALID_CREDENTIALS)
	                    .build();
	        }
	    } else {
	        LOG.warning(LOG_MESSAGE_UNKNOW_USER + data.username);
	        return Response.status(Status.FORBIDDEN)
	                .entity(MESSAGE_INVALID_CREDENTIALS)
	                .build();
	    }
	}

	

}