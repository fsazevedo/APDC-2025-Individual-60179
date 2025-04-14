package avaliacao.resources;

import java.util.*;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import avaliacao.util.AuthToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Path("/listusers")
public class ListUsersResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson g = new Gson();

    private static final String[] expectedFields = {
       "user_name", "user_email", "user_role", "user_account_state", "profile_type",
        "user_creation_time", "user_login_time", "user_employer", "user_employer_nif",
        "user_job", "user_address", "user_nif"
    };

    @POST
    @Path("/v1")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(@Context HttpHeaders headers) {
        try {
            String auth = headers.getHeaderString("Authorization");
            if (auth == null || auth.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token em falta").build();
            }

            AuthToken token = g.fromJson(auth, AuthToken.class);
            if (token == null || !token.isValid() || token.isRevoked(datastore)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou sessão terminada").build();
            }

            Key requesterKey = datastore.newKeyFactory().setKind("User").newKey(token.user);
            Entity requester = datastore.get(requesterKey);

            if (requester == null) {
                return Response.status(Response.Status.FORBIDDEN).entity("Utilizador autenticado não encontrado").build();
            }

            String requesterRole = requester.getString("user_role");

            Query<Entity> query;
            if (requesterRole.equals("ADMIN")) {
                query = Query.newEntityQueryBuilder().setKind("User").build();
            } else if (requesterRole.equals("BACKOFFICE")) {
                query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.PropertyFilter.eq("user_role", "ENDUSER"))
                        .build();
            } else if (requesterRole.equals("ENDUSER")) {
                query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.CompositeFilter.and(
                                StructuredQuery.PropertyFilter.eq("user_role", "ENDUSER"),
                                StructuredQuery.PropertyFilter.eq("profile_type", "publico"),
                                StructuredQuery.PropertyFilter.eq("user_account_state", "ATIVO")
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Role não autorizado para listagem").build();
            }

            QueryResults<Entity> results = datastore.run(query);
            List<JsonObject> users = new ArrayList<>();

            while (results.hasNext()) {
                Entity user = results.next();
                JsonObject obj = new JsonObject();

                if (requesterRole.equals("ENDUSER")) {
                    obj.addProperty("username", getOrDefault(user, "user_id"));
                    obj.addProperty("email", getOrDefault(user, "user_email"));
                    obj.addProperty("name", getOrDefault(user, "user_name"));
                } else {
                	obj.addProperty("username", user.getKey().getName());
                    for (String field : expectedFields) {
                        String value = user.contains(field) ? user.getValue(field).get().toString() : "NOT DEFINED";
                        obj.addProperty(field, cleanQuotes(value));
                    }
                }

                users.add(obj);
            }

            return Response.ok(g.toJson(users)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro interno: " + e.getMessage())
                    .build();
        }
    }


    private String getOrDefault(Entity e, String field) {
        return e.contains(field) ? e.getString(field) : "NOT DEFINED";
    }

    private String cleanQuotes(String input) {
        return input.replaceAll("^\"|\"$", "");
    }
}
