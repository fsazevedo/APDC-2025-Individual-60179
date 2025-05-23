package avaliacao.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import avaliacao.util.AuthToken;
import avaliacao.util.RemoveUserData;

import com.google.gson.Gson;

@Path("/removeuser")
public class RemoveUserAccountResource {
	
    private static final String ENDUSER = "ENDUSER";
    private static final String BACKOFFICE = "BACKOFFICE";
    private static final String ADMIN = "ADMIN";
    private static final String PARTNER = "PARTNER";
    

    private static final Logger LOG = Logger.getLogger(RemoveUserAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory keyFactory = datastore.newKeyFactory().setKind("User");
    private final Gson g = new Gson();

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUser(@Context HttpHeaders headers, RemoveUserData data) {
        try {
            String auth = headers.getHeaderString("Authorization");
            if (auth == null || auth.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token em falta").build();
            }

            AuthToken token = g.fromJson(auth, AuthToken.class);
            if (token == null || !token.isValid() || token.isRevoked(datastore)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou sessão terminada").build();
            }

            Key requesterKey = keyFactory.newKey(token.user);
            Entity requester = datastore.get(requesterKey);
            if (requester == null) {
                return Response.status(Response.Status.FORBIDDEN).entity("Utilizador autenticado não existe").build();
            }

            if (!requester.contains("user_role")) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("O utilizador autenticado não tem role definido.").build();
            }

            String requesterRole = requester.getString("user_role");

            Key targetKey = null;
            Entity target = null;

            Key possibleKey = keyFactory.newKey(data.identifier);
            Entity possibleEntity = datastore.get(possibleKey);
            if (possibleEntity != null) {
                targetKey = possibleKey;
                target = possibleEntity;
            } else {
                Query<Entity> query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.PropertyFilter.eq("user_email", data.identifier))
                        .build();

                QueryResults<Entity> results = datastore.run(query);
                List<Entity> matched = new ArrayList<>();
                while (results.hasNext()) {
                    matched.add(results.next());
                }

                if (matched.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("Nenhum utilizador encontrado com esse identificador.")
                            .build();
                } else if (matched.size() > 1) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity("Mais do que um utilizador com esse email. Remoção ambígua.")
                            .build();
                } else {
                    target = matched.get(0);
                    targetKey = target.getKey();
                }
            }

            if (!target.contains("user_role")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Utilizador alvo não tem role definido.").build();
            }

            String targetRole = target.getString("user_role");

            if (requesterRole.equals("ADMIN")) {
                // Tudo ok
            } else if (requesterRole.equals(BACKOFFICE)) {
                if (!targetRole.equals("ENDUSER") && !targetRole.equals(PARTNER)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("BACKOFFICE só pode remover contas com role ENDUSER ou PARTNER.")
                            .build();
                }
            } else {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Sem permissões para remover contas.")
                        .build();
            }

            datastore.delete(targetKey);
            LOG.info("Conta do utilizador removida: " + targetKey.getName() + " por " + token.user);
            return Response.ok("Conta removida com sucesso").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro interno: " + e.getMessage())
                    .build();
        }
    }

}
