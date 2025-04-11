package avaliacao.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import avaliacao.util.AuthToken;

import com.google.gson.Gson;

@Path("/welcome")
public class FrontPageResource {

    private final Gson g = new Gson();

    @GET
    @Path("/html")
    @Produces(MediaType.TEXT_HTML)
    public Response welcomeHTML(@Context HttpHeaders headers) {
        String auth = headers.getHeaderString("Authorization");
        if (auth == null || auth.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta o token.").build();
        }

        AuthToken token = g.fromJson(auth, AuthToken.class);

        String html = "<!DOCTYPE html>" +
                "<html lang='pt'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<title>Bem-vindo</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 40px; }" +
                ".container { background: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; box-shadow: 0 0 15px rgba(0,0,0,0.2); }" +
                "h1 { color: #2c3e50; }" +
                ".info { margin-bottom: 20px; }" +
                ".role { font-weight: bold; font-size: 18px; color: #2980b9; }" +
                ".actions { margin-top: 30px; }" +
                ".button { display: inline-block; padding: 10px 20px; margin-right: 10px; background: #3498db; color: white; border-radius: 5px; text-decoration: none; }" +
                ".button:hover { background: #2980b9; }" +
                "</style>" +
                "</head><body>" +
                "<div class='container'>" +
                "<h1>Bem-vindo, " + token.user + "!</h1>" +
                "<div class='info'>" +
                "<p class='role'>Papel atual: " + token.role + "</p>" +
                "<p><strong>Sessão iniciada em:</strong> " + new java.util.Date(token.creationData) + "</p>" +
                "<p><strong>Expira em:</strong> " + new java.util.Date(token.expirationData) + "</p>" +
                "</div>" +
                "<div class='actions'>" +
                gerarAcoesPorPapel(token.role) +
                "</div>" +
                "</div>" +
                "</body></html>";

        return Response.ok(html).build();
    }
    
    private String gerarAcoesPorPapel(String role) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return "<a href='/admin/createUser' class='button'>Criar Utilizador</a>" +
                       "<a href='/admin/viewLogs' class='button'>Ver Logs</a>";
            case "USER":
                return "<a href='/user/profile' class='button'>Ver Perfil</a>" +
                       "<a href='/user/update' class='button'>Atualizar Dados</a>";
            case "GUEST":
                return "<a href='/guest/info' class='button'>Explorar</a>";
            default:
                return "<a href='/' class='button'>Página Principal</a>";
        }
    }


    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response welcomeJSON(@Context HttpHeaders headers) {
        String auth = headers.getHeaderString("Authorization");
        if (auth == null || auth.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta o token.").build();
        }

        AuthToken token = g.fromJson(auth, AuthToken.class);

        return Response.ok(g.toJson(token)).build();
    }
}
