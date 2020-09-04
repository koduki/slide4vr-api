/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.Authenticated;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import dev.nklab.jl2.web.profile.WebTrace;

/**
 *
 * @author koduki
 */
@Path("/secured")
@RequestScoped
public class SecuredCheckResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @WebTrace
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context SecurityContext ctx) throws JsonProcessingException {
        var caller = ctx.getUserPrincipal();
        var result = Map.of(
                "id", caller == null ? "anonymous" : caller.getName(),
                "name", jwt.getClaim("name"),
                "picture", jwt.getClaim("picture"),
                "expiration_time", jwt.getExpirationTime(),
                "has_jwt", jwt.getClaimNames() != null,
                "payload", jwt.getClaimNames().stream()
                        .map(k -> List.of(k, jwt.getClaim(k).toString()))
                        .collect(Collectors.toMap(x -> x.get(0), x -> x.get(1)))
        );

        return Response.ok(new ObjectMapper().writeValueAsString(result))
                .build();
    }

}
