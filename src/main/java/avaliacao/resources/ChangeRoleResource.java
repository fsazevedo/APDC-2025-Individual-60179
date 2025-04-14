package avaliacao.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import avaliacao.util.AuthToken;
import avaliacao.util.ChangeRoleData;
import com.google.gson.Gson;

@Path("/changerole")
public class ChangeRoleResource {
	
    private static final String ENDUSER = "ENDUSER";
    private static final String BACKOFFICE = "BACKOFFICE";
    private static final String ADMIN = "ADMIN";
    private static final String PARTNER = "PARTNER";

    private static final Logger LOG = Logger.getLogger(ChangeRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory keyFactory = datastore.newKeyFactory().setKind("User");
    private final Gson g = new Gson();
    
    

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeUserRole(@Context HttpHeaders headers, ChangeRoleData data) {
        try {
            String auth = headers.getHeaderString("Authorization");
            if (auth == null || auth.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token em falta").build();
            }
            

            AuthToken token = g.fromJson(auth, AuthToken.class);
            if (token == null || !token.isValid() || token.isRevoked(datastore)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou expirado").build();
            }

            Key requesterKey = keyFactory.newKey(token.user);
            Entity requester = datastore.get(requesterKey);
            if (requester == null) {
                return Response.status(Response.Status.FORBIDDEN).entity("Utilizador autenticado não encontrado").build();
            }

            if (!requester.contains("user_role")) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity("A conta do utilizador autenticado não tem role definido.")
                               .build();
            }

            String requesterRole = requester.getString("user_role"); 
            
            if (data.username == null || data.newRole == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Campos username ou newRole em falta").build();
            }

            Key targetKey = keyFactory.newKey(data.username);
            Entity target = datastore.get(targetKey);
            if (target == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Utilizador alvo não existe").build();
            }

            if (!target.contains("user_role")) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity("Utilizador alvo não tem role definido.")
                               .build();
            }

            String currentRole = target.getString("user_role");
            String newRole = data.newRole.toUpperCase();
            if (!newRole.equals(ENDUSER) && !newRole.equals(PARTNER) &&
            	    !newRole.equals(BACKOFFICE) && !newRole.equals(ADMIN)) {
            	    return Response.status(Response.Status.BAD_REQUEST).entity("Role inválido: " + newRole).build();
            	}

            // Verificação de permissões
            if (requesterRole.equals("ADMIN")) {
                // Pode tudo
            } else if (requesterRole.equals("BACKOFFICE")) {
                boolean validChange = (currentRole.equals(ENDUSER) && newRole.equals(PARTNER)) ||
                                      (currentRole.equals(PARTNER) && newRole.equals(ENDUSER));
                if (!validChange) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("BACKOFFICE só pode alterar entre ENDUSER e PARTNER")
                            .build();
                }
            } else {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Utilizador sem permissões para alterar roles")
                        .build();
            }

            // Atualiza role
            Entity updatedTarget = Entity.newBuilder(target)
                    .set("user_role", newRole)
                    .build();
            datastore.update(updatedTarget);

            LOG.info("Utilizador " + data.username + " alterado de " + currentRole + " para " + newRole + " por " + token.user);
            return Response.ok("Role atualizado com sucesso").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro interno: " + e.getMessage())
                    .build();
        }
    }
}
