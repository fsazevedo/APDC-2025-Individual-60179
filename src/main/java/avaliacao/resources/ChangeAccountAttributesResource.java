package avaliacao.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import avaliacao.util.AuthToken;
import avaliacao.util.ChangeAttributesData;
import com.google.gson.Gson;

@Path("/changeattributes")
public class ChangeAccountAttributesResource {

    private static final Logger LOG = Logger.getLogger(ChangeAccountAttributesResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson g = new Gson();

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeAttributes(@Context HttpHeaders headers, ChangeAttributesData data) {
        String auth = headers.getHeaderString("Authorization");
        if (auth == null || auth.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token em falta").build();
        }

        AuthToken token = g.fromJson(auth, AuthToken.class);
        if (token == null || !token.isValid() || token.isRevoked(datastore)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou expirado").build();
        }
        KeyFactory keyFactory = datastore.newKeyFactory().setKind("User");

        Key requesterKey = keyFactory.newKey(token.user);
        Entity requester = datastore.get(requesterKey);
        if (requester == null) {
            return Response.status(Response.Status.FORBIDDEN).entity("Requester não encontrado").build();
        }

        String requesterRole = requester.getString("user_role");
        Key targetKey = keyFactory.newKey(data.targetUsername);
        Entity target = datastore.get(targetKey);
        
        

        if (target == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Utilizador alvo não existe").build();
        }
     // Impedir que BACKOFFICE altere username ou email
        if (requesterRole.equals("BACKOFFICE")) {
            if (data.newUsername != null && !data.newUsername.equals(data.targetUsername)) {
                return Response.status(Response.Status.FORBIDDEN).entity("BACKOFFICE não pode alterar o username").build();
            }
            if (data.email != null) {
                return Response.status(Response.Status.FORBIDDEN).entity("BACKOFFICE não pode alterar o email").build();
            }
        }

        // Impedir que ENDUSER altere email, username, role ou estado
        if (requesterRole.equals("ENDUSER")) {
            if (data.newUsername != null || data.email != null || data.role != null || data.accountState != null) {
                return Response.status(Response.Status.FORBIDDEN).entity("ENDUSER não pode alterar username, email, role ou estado").build();
            }
        }

        String targetRole = target.getString("user_role");
        String targetState = target.getString("user_account_state");

        if (requesterRole.equals("ENDUSER")) {
            if (!token.user.equals(data.targetUsername)) {
                return Response.status(Response.Status.FORBIDDEN).entity("ENDUSER só pode alterar a própria conta").build();
            }
        } else if (requesterRole.equals("BACKOFFICE")) {
            if (!(targetRole.equals("ENDUSER") || targetRole.equals("PARTNER"))) {
                return Response.status(Response.Status.FORBIDDEN).entity("BACKOFFICE só pode alterar ENDUSER ou PARTNER").build();
            }
            if (!targetState.equals("ATIVADA")) {
                return Response.status(Response.Status.FORBIDDEN).entity("Conta alvo não está ativa").build();
            }
        }

        boolean isAdmin = requesterRole.equals("ADMIN");
        boolean isEnduser = requesterRole.equals("ENDUSER");

        if (isAdmin && data.newUsername != null && !data.newUsername.isBlank() && !data.newUsername.equals(data.targetUsername)) {
            Key newKey = keyFactory.newKey(data.newUsername);
            if (datastore.get(newKey) != null) {
                return Response.status(Response.Status.CONFLICT).entity("Novo username já existe").build();
            }

            Entity.Builder newBuilder = Entity.newBuilder(newKey);
            for (String name : target.getNames()) {
                Value<?> value = target.getValue(name);
                newBuilder.set(name, value);
            }

            if (data.name != null) newBuilder.set("user_name", StringValue.of(data.name));
            if (data.phoneNumber != null) newBuilder.set("user_phone", StringValue.of(data.phoneNumber));
            if (data.address != null) newBuilder.set("user_address", StringValue.of(data.address));
            if (data.job != null) newBuilder.set("user_job", StringValue.of(data.job));
            if (data.employer != null) newBuilder.set("user_employer", StringValue.of(data.employer));
            if (data.employerNif != null) newBuilder.set("user_employer_nif", StringValue.of(data.employerNif));
            if (data.nif != null) newBuilder.set("user_nif", StringValue.of(data.nif));
            if (data.profileType != null) newBuilder.set("profile_type", StringValue.of(data.profileType));
            if (data.role != null) newBuilder.set("user_role", StringValue.of(data.role));
            if (data.accountState != null) newBuilder.set("user_account_state", StringValue.of(data.accountState));

            Transaction txn = datastore.newTransaction();
            try {
                txn.put(newBuilder.build());
                txn.delete(targetKey);
                txn.commit();
                LOG.info("Username alterado de " + data.targetUsername + " para " + data.newUsername);
                return Response.ok("Username alterado com sucesso").build();
            } catch (Exception e) {
                if (txn.isActive()) txn.rollback();
                return Response.serverError().entity("Erro ao mudar username: " + e.getMessage()).build();
            }
        } else {
            Entity.Builder builder = Entity.newBuilder(target);

            if (!isEnduser || (isEnduser && data.name == null)) {
                if (data.name != null) builder.set("user_name", StringValue.of(data.name));
            }
            if (data.phoneNumber != null) builder.set("user_phone", StringValue.of(data.phoneNumber));
            if (data.address != null) builder.set("user_address", StringValue.of(data.address));
            if (data.job != null) builder.set("user_job", StringValue.of(data.job));
            if (data.employer != null) builder.set("user_employer", StringValue.of(data.employer));
            if (data.employerNif != null) builder.set("user_employer_nif", StringValue.of(data.employerNif));
            if (data.nif != null) builder.set("user_nif", StringValue.of(data.nif));
            if (data.profileType != null) builder.set("profile_type", StringValue.of(data.profileType));

            if (isAdmin) {
                if (data.role != null) builder.set("user_role", StringValue.of(data.role));
                if (data.accountState != null) builder.set("user_account_state", StringValue.of(data.accountState));
            }

            datastore.update(builder.build());

            LOG.info("Alteracoes feitas para utilizador: " + data.targetUsername);
            return Response.ok("Atributos atualizados com sucesso.").build();
        }
    }
}
