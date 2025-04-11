package avaliacao.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import avaliacao.util.AuthToken;
import avaliacao.util.ChangeAccountStateData;
import com.google.gson.Gson;

@Path("/changeaccountstate")
public class ChangeAccountStateResource {

    private static final Logger LOG = Logger.getLogger(ChangeAccountStateResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory keyFactory = datastore.newKeyFactory().setKind("User");
    private final Gson g = new Gson();

    // Constantes de estados
    private static final String STATE_ATIVADA = "ATIVADA";
    private static final String STATE_DESATIVADA = "DESATIVADA";
    private static final String STATE_SUSPENSA = "SUSPENSA";

    // Constantes de roles
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_BACKOFFICE = "BACKOFFICE";

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeAccountState(@Context HttpHeaders headers, ChangeAccountStateData data) {
        try {
            String auth = headers.getHeaderString("Authorization");
            if (auth == null || auth.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token em falta").build();
            }

            AuthToken token = g.fromJson(auth, AuthToken.class);
            if (token == null || !token.isValid() || token.isRevoked(datastore)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou expirado").build();
            }

            if (data.username == null || data.newState == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Campos obrigatórios em falta").build();
            }

            Key requesterKey = keyFactory.newKey(token.user);
            Entity requester = datastore.get(requesterKey);
            if (requester == null) {
                return Response.status(Response.Status.FORBIDDEN).entity("Utilizador autenticado não encontrado").build();
            }

            String requesterRole = requester.contains("user_role") ? requester.getString("user_role") : "UNKNOWN";

            Key targetKey = keyFactory.newKey(data.username);
            Entity target = datastore.get(targetKey);
            if (target == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Utilizador alvo não existe").build();
            }

            String currentState = target.contains("user_account_state") ? target.getString("user_account_state") : "UNKNOWN";
            String newState = data.newState.toUpperCase();

            // Validar estados possíveis
            if (!newState.equals(STATE_ATIVADA) && !newState.equals(STATE_DESATIVADA) && !newState.equals(STATE_SUSPENSA)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Estado inválido: " + newState).build();
            }

            if (requesterRole.equals(ROLE_ADMIN)) {
                // Pode tudo
            } else if (requesterRole.equals(ROLE_BACKOFFICE)) {
                boolean validChange =
                        (currentState.equals(STATE_ATIVADA) && newState.equals(STATE_DESATIVADA)) ||
                        (currentState.equals(STATE_DESATIVADA) && newState.equals(STATE_ATIVADA));
                if (!validChange) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("BACKOFFICE só pode alternar entre ATIVADA e DESATIVADA")
                            .build();
                }
            } else {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Utilizador sem permissões para alterar o estado da conta")
                        .build();
            }

            // Atualizar estado da conta
            Entity updatedTarget = Entity.newBuilder(target)
                    .set("user_account_state", newState)
                    .build();
            datastore.update(updatedTarget);

            LOG.info("Estado da conta de " + data.username + " alterado de " + currentState + " para " + newState + " por " + token.user);
            return Response.ok("Estado da conta alterado com sucesso").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro interno: " + e.getMessage())
                    .build();
        }
    }
}
