package avaliacao.resources;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

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
import avaliacao.util.ChangePasswordData;

@Path("/changepassword")
public class ChangePasswordResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(@Context HttpHeaders headers, ChangePasswordData data) {
        // Extrai o token de autorização do header
        AuthToken token = AuthToken.extractFromHeader(headers);
        if (token == null || !token.isValid() || token.isRevoked(datastore)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou expirado").build();
        }

        Key userKey = userKeyFactory.newKey(token.user);
        Entity user = datastore.get(userKey);
        if (user == null) {
            return Response.status(Status.NOT_FOUND).entity("Utilizador não existe.").build();
        }

        // Verifica se a password atual está correta
        String storedHash = user.getString("user_pwd");
        if (!storedHash.equals(DigestUtils.sha512Hex(data.oldPassword))) {
            return Response.status(Status.FORBIDDEN).entity("Password atual incorreta.").build();
        }

        // Verifica se nova password coincide com a confirmação
        if (!data.newPassword.equals(data.confirmNewPassword)) {
            return Response.status(Status.BAD_REQUEST).entity("Nova password não coincide com confirmação.").build();
        }

        // Atualiza a password
        Entity updatedUser = Entity.newBuilder(user)
            .set("user_pwd", DigestUtils.sha512Hex(data.newPassword))
            .build();
        datastore.update(updatedUser);

        return Response.ok().entity("Password alterada com sucesso.").build();
    }
}
