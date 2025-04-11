package avaliacao.resources;

import java.util.logging.Logger;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import avaliacao.util.AuthToken;
import avaliacao.util.WorkSheetData;

@Path("/worksheets")
public class CreateWorkSheetResource {

    private static final Logger LOG = Logger.getLogger(CreateWorkSheetResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson g = new Gson();

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrUpdateWorksheet(@Context HttpHeaders headers, WorkSheetData data) {
        String auth = headers.getHeaderString("Authorization");
        if (auth == null || auth.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token em falta").build();
        }

        AuthToken token;
        try {
            token = g.fromJson(auth, AuthToken.class);
            if (token == null || !token.isValid() || token.isRevoked(datastore)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido ou expirado").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.FORBIDDEN).entity("Token inválido.").build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.user);
        Entity user = datastore.get(userKey);
        if (user == null) {
            return Response.status(Response.Status.FORBIDDEN).entity("Utilizador não encontrado.").build();
        }

        String userRole = user.getString("user_role");
        Key worksheetKey = datastore.newKeyFactory().setKind("WorkSheet").newKey(data.reference);

        // BACKOFFICE: cria ou atualiza folha de obra (exceto estado de obra)
        if (userRole.equals("BACKOFFICE")) {
            Entity.Builder builder = Entity.newBuilder(worksheetKey)
                .set("reference", data.reference)
                .set("descricao", data.descricao)
                .set("tipo_alvo", data.tipoAlvo)
                .set("estado_adjudicacao", data.estadoAdjudicacao);

            if (data.estadoAdjudicacao.equals("ADJUDICADO")) {
                builder.set("data_adjudicacao", Timestamp.parseTimestamp(data.dataAdjudicacao))
                       .set("data_inicio_prevista", Timestamp.parseTimestamp(data.dataInicioPrevista))
                       .set("data_conclusao_prevista", Timestamp.parseTimestamp(data.dataConclusaoPrevista))
                       .set("entidade_adjudicada", data.entidadeAdjudicada)
                       .set("nif_empresa", data.nifEmpresa)
                       .set("partner_id", data.partnerId);
            }

            datastore.put(builder.build());
            return Response.ok("Folha de obra registada com sucesso.").build();
        }

        // PARTNER: pode apenas alterar estado da obra e observações se for o responsável
        if (userRole.equals("PARTNER")) {
            Entity worksheet = datastore.get(worksheetKey);
            if (worksheet == null || !worksheet.contains("partner_id") ||
                !worksheet.getString("partner_id").equals(token.user)) {
                return Response.status(Response.Status.FORBIDDEN).entity("Sem permissões para alterar esta obra.").build();
            }

            Entity updated = Entity.newBuilder(worksheet)
                .set("estado_obra", data.estadoObra)
                .set("observacoes", data.observacoes == null ? "" : data.observacoes)
                .build();

            datastore.update(updated);
            return Response.ok("Estado da obra atualizado com sucesso.").build();
        }

        return Response.status(Response.Status.FORBIDDEN).entity("Role sem permissão para esta operação.").build();
    }


}