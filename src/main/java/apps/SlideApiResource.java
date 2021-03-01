package apps;

import static dev.nklab.jl2.Extentions.$;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import dev.nklab.jl2.web.logging.Logger;
import dev.nklab.jl2.web.profile.WebTrace;
import io.quarkus.security.Authenticated;

@Path("/api/slide")
public class SlideApiResource {

    private final Logger logger = Logger.getLogger("slide4vr");

    @Inject
    SlideService slideService;

    @Inject
    Pptx2pngService pptx2pngService;

    @Inject
    TokenService tokenService;

    // @Provider
    // public static class ForbiddenExceptionMapper implements ExceptionMapper<RuntimeException> {
    //     @Override
    //     public Response toResponse(RuntimeException exception) {
    //         return Response.status(Response.Status.FORBIDDEN).build();
    //     }
    // }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response list(@HeaderParam("x-slide4vr-auth") final String token) throws IOException, ParseException {
        // var userId = "0m3ItnvCMQhbACV9rR5mkdmFOns2";

        // System.out.println(token2);
        // var token = generate(userId);

        final var userId = tokenService.getUserId(token);
        System.out.println(userId);
        System.out.println(tokenService.getToken(userId));

        logger.debug("getList", $("id", userId));

        final var slides = slideService.listSlides(userId);
        return Response.ok(new ObjectMapper().writeValueAsString(slides)).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Path("user")
    public Response getUser(@HeaderParam("x-slide4vr-auth") final String token) throws IOException, ParseException {
        // var userId = "0m3ItnvCMQhbACV9rR5mkdmFOns2";

        // System.out.println(token2);
        // var token = generate(userId);

        final var userId = tokenService.getUserId(token);
        // System.out.println(userId);
        // System.out.println(tokenService.getToken(userId));

        // logger.debug("getList", $("id", userId));


        return Response.ok(new ObjectMapper().writeValueAsString(Map.of("userId", userId))).build();
    }

    @GET
    @Path("{id}/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response get(@PathParam("id") final String id, @PathParam("key") final String key,
            @QueryParam("format") String format) throws IOException {
        var json = "";
        if (format == null) {
            format = "";
        }
        switch (format) {
        case "vcas":
            final var printer = new DefaultPrettyPrinter();
            printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
            json = new ObjectMapper().writer(printer).writeValueAsString(slideService.getSlide4Vcas(id, key));
            break;
        default:
            json = new ObjectMapper().writeValueAsString(slideService.getSlide(id, key));
        }

        return Response.ok(json).build();
    }

    @POST
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response create(@HeaderParam("x-slide4vr-auth") final String token, final MultipartFormDataInput form)
            throws IOException {
        final var slide = new SlideFormBean(form.getFormDataMap());

        final var userId = tokenService.getUserId(token);
        final var key = slideService.create(userId, slide);
        pptx2pngService.request(userId, key, slide.getContentType(), slide.getExtention());

        return Response.ok(String.format("{message:'%s', data-size:'%d'}", slide.getTitle(), slide.getSlide().length))
                .build();
    }

    @DELETE
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    public Response delete(@Context final SecurityContext ctx, @PathParam("key") final String key) throws IOException {
        final var id = ctx.getUserPrincipal().getName();
        final var result = slideService.delete(id, key);

        return Response.ok(String.format("{delete:'%s', status: %s}", key, result)).build();
    }
}
