package apps;

import static dev.nklab.jl2.Extentions.$;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import dev.nklab.jl2.web.logging.Logger;
import dev.nklab.jl2.web.profile.WebTrace;
import fw.AuthException;

@Path("/api/slide")
public class SlideApiResource {

    private final Logger logger = Logger.getLogger("slide4vr");

    @Inject
    SlideService slideService;

    @Inject
    Pptx2pngService pptx2pngService;

    @Inject
    TokenService tokenService;

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
                json = new ObjectMapper().writeValueAsString(slideService.getSlide4Vcas(id, key));
                break;
            default:
                json = new ObjectMapper().writeValueAsString(slideService.getSlide(id, key));
        }

        if (!json.isEmpty()) {
            return Response.ok(json).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity("Not Found: " + id + "/" + key).build();
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response list(@HeaderParam("x-slide4vr-auth") final String token)
            throws IOException, ParseException {
        if (token == null || token.isBlank()) {
            throw new AuthException("token is empty");
        }
        var userId = tokenService.getUserId(token);

        logger.debug("getList", $("id", userId));

        final var slides = slideService.listSlides(userId);
        return Response.ok(new ObjectMapper().writeValueAsString(slides)).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Path("user")
    public Response getUser(@HeaderParam("x-slide4vr-auth") final String token)
            throws IOException, ParseException {
        if (token == null || token.isBlank()) {
            throw new AuthException("token is empty");
        }
        var userId = tokenService.getUserId(token);
        return Response.ok(new ObjectMapper().writeValueAsString(Map.of("userId", userId))).build();
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response create(@HeaderParam("x-slide4vr-auth") final String token,
            final MultipartFormDataInput form) throws IOException {
        var slideForm = new SlideFormBean(form.getFormDataMap());
        if (token == null || token.isBlank()) {
            throw new AuthException("token is empty");
        }
        var userId = tokenService.getUserId(token);
        var key = slideService.create(userId, slideForm);
        pptx2pngService.request(userId, key, slideForm.getContentType(), slideForm.getExtention());

        return Response.ok(new ObjectMapper()
                .writeValueAsString(Map.of("title", slideForm.getTitle(), "length",
                        slideForm.getSlide().length, "path", "/api/slide/" + userId + "/" + key)))
                .build();
    }

    @DELETE
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response delete(@HeaderParam("x-slide4vr-auth") final String token,
            @PathParam("key") final String key) throws IOException {
        if (token == null || token.isBlank()) {
            throw new AuthException("token is empty");
        }
        var id = tokenService.getUserId(token);
        var result = slideService.delete(id, key);

        return Response.ok(String.format("{delete:'%s', status: %s}", key, result)).build();
    }
}
