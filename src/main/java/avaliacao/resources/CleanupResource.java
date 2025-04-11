package avaliacao.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/cleanup")
public class CleanupResource {

    private static final Logger LOG = Logger.getLogger(CleanupResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/all")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteAllUsers() {
        try {
            Query<Key> query = Query.newKeyQueryBuilder().setKind("User").build();
            QueryResults<Key> keys = datastore.run(query);

            int count = 0;
            while (keys.hasNext()) {
                datastore.delete(keys.next());
                count++;
            }

            LOG.info("Cleanup: Apagados " + count + " utilizadores.");
            return Response.ok("Cleanup completo. Foram removidos " + count + " utilizadores.").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao limpar utilizadores: " + e.getMessage()).build();
        }
    }
}
