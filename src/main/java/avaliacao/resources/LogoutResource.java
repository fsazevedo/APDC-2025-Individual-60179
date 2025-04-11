package avaliacao.resources;

import java.util.logging.Logger;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import avaliacao.util.AuthToken;

@Path("/logout")
public class LogoutResource {

    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson g = new Gson();

    @POST
    @Path("/v1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response logout(@Context HttpHeaders headers) {
        String auth = headers.getHeaderString("Authorization");
        if (auth == null || auth.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token em falta").build();
        }

        AuthToken token;
        try {
            token = g.fromJson(auth, AuthToken.class);
            if (token == null || !token.isValid() || token.isRevoked(datastore)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou sessão terminada").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.FORBIDDEN).entity("Token inválido.").build();
        }

        Key revokedKey = datastore.newKeyFactory()
            .setKind("RevokedToken")
            .newKey(token.verifier);

        Entity revoked = Entity.newBuilder(revokedKey)
            .set("username", token.user)
            .set("revoked_on", Timestamp.now())
            .build();

        datastore.put(revoked);
        LOG.info("Token revogado para utilizador: " + token.user);

        return Response.ok("Logout efetuado com sucesso.").build();
    }

}