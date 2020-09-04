package apps;

import static dev.nklab.jl2.Extentions.*;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.Authenticated;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import dev.nklab.jl2.web.profile.WebTrace;
import dev.nklab.jl2.web.logging.Logger;
import javax.ws.rs.DELETE;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/slide")
public class SlideResource {

    private final Logger logger = Logger.getLogger("slide4vr");

    @Inject
    JsonWebToken jwt;

    @Inject
    SlideService slideService;

    @Inject
    Pptx2pngService pptx2pngService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    public Response list(@Context SecurityContext ctx) throws IOException {
        var id = ctx.getUserPrincipal().getName();
        logger.debug("getList", $("id", id));

        var slides = slideService.listSlides(id);
        return Response.ok(new ObjectMapper().writeValueAsString(slides))
                .build();
    }

    @GET
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    public Response get(@Context SecurityContext ctx, @PathParam("key") String key) throws IOException {
        var id = ctx.getUserPrincipal().getName();

        var printer = new DefaultPrettyPrinter();
        printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        var item = slideService.getSlide(id, key);
        var json = new ObjectMapper().writer(printer).writeValueAsString(item);

        return Response.ok(json)
                .build();
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    public Response create(@Context SecurityContext ctx, MultipartFormDataInput form) throws IOException {
        var slide = new SlideFormBean(form.getFormDataMap());

        var id = ctx.getUserPrincipal().getName();
        var key = slideService.create(id, slide);
        pptx2pngService.request(id, key, slide.getContentType(), slide.getExtention());

        return Response.ok(
                String.format("{message:'%s', data-size:'%d'}",
                        slide.getTitle(),
                        slide.getSlide().length))
                .build();
    }

    @DELETE
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    public Response delete(@Context SecurityContext ctx, @PathParam("key") String key) throws IOException {
        var id = ctx.getUserPrincipal().getName();
        var result = slideService.delete(id, key);

        return Response.ok(String.format("{delete:'%s', status: %s}", key, result))
                .build();
    }
}
