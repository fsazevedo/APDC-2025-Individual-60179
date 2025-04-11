package avaliacao.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import avaliacao.util.LoginData;
import avaliacao.util.RegisterData;
import avaliacao.util.RegisterExtraData;

@Path("/register")
public class RegisterResource {

    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUserV1(RegisterData data) {
        LOG.info("RegisterUserV1 method called");
        LOG.fine("Attempt to register user: " + data.username);
        LOG.fine("Received data: " + data.toString());
        
        LOG.info("====== REGISTO DEBUG INFO ======");
        LOG.info("username: " + data.username);
        LOG.info("email: " + data.email);
        LOG.info("name: " + data.name);
        LOG.info("phoneNumber: " + data.phoneNumber);
        LOG.info("password: " + data.password);
        LOG.info("confirmation: " + data.confirmation);
        LOG.info("profileType: " + data.profileType);
        LOG.info("extraData: " + (data.extraData != null ? data.extraData.toString() : "null"));
        LOG.info("================================");

        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        RegisterExtraData extraData = data.extraData;
        if (extraData != null && !extraData.validOptionalFields()) {
            return Response.status(Status.BAD_REQUEST).entity("Invalid optional fields.").build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
            Entity user = txn.get(userKey);

            if (user != null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User already exists.").build();
            } else {
                Entity.Builder userBuilder = Entity.newBuilder(userKey)
                        .set("user_name", data.name)
                        .set("user_pwd", DigestUtils.sha512Hex(data.password))
                        .set("user_creation_time", Timestamp.now())
                        .set("user_email", data.email != null ? data.email : "NOT DEFINED")
                        .set("user_phone", data.phoneNumber != null ? data.phoneNumber : "NOT DEFINED")
                        .set("profile_type", data.profileType != null ? data.profileType : "NOT DEFINED")
                        .set("user_account_state", "ATIVO")
                        .set("user_role", "ENDUSER");

                if (extraData != null) {
                    extraData.addOptionalFieldsToBuilder(userBuilder);
                }

                user = userBuilder.build();
                txn.put(user);
                txn.commit();
                LOG.info("User registered " + data.username);
                return Response.ok().build();
            }
        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}
