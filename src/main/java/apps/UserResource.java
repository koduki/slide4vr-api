package apps;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.nklab.jl2.web.logging.Logger;
import dev.nklab.jl2.web.profile.WebTrace;
import io.quarkus.security.Authenticated;

@Path("/user")
public class UserResource {

    private final Logger logger = Logger.getLogger("slide4vr");

    @Inject
    JsonWebToken jwt;

    @Inject
    TokenService tokenService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    @Path("apptoken")
    public Response getAppToken(@Context SecurityContext ctx) throws IOException, ParseException {
        var userId = ctx.getUserPrincipal().getName();

        var token = Map.of("token", tokenService.getToken(userId));
        return Response.ok(new ObjectMapper().writeValueAsString(token)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    @Path("apptoken/reset")
    public Response resetAppToken(@Context SecurityContext ctx) throws IOException, ParseException {
        var userId = ctx.getUserPrincipal().getName();
System.out.println(userId);
        var token = Map.of("token", tokenService.generate(userId));
        return Response.ok(new ObjectMapper().writeValueAsString(token)).build();
    }
}