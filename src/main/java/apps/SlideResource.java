package apps;

import static dev.nklab.jl2.Extentions.*;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.Authenticated;
import java.io.IOException;
import javax.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import dev.nklab.jl2.web.profile.WebTrace;
import dev.nklab.jl2.web.logging.Logger;
import java.text.ParseException;
import javax.ws.rs.DELETE;
import javax.ws.rs.QueryParam;
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

    @ConfigProperty(name = "slide4vr.healthcheck.url")
    String healthcheckUrl;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    @Authenticated
    public Response list(@Context SecurityContext ctx) throws IOException, ParseException {
        var id = ctx.getUserPrincipal().getName();
        logger.debug("getList", $("id", id));

        preSpinUp();

        var slides = slideService.listSlides(id);
        return Response.ok(new ObjectMapper().writeValueAsString(slides))
                .build();
    }

    private void preSpinUp() {
//        var client = HttpClient.newHttpClient();
//        var request = HttpRequest.newBuilder().GET().version(HttpClient.Version.HTTP_1_1)
//                .header("Content-Type", "application/json").uri(URI.create(healthcheckUrl));
//
//        client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
//                .thenApply(HttpResponse::body).thenAccept(System.out::println);
    }

    @GET
    @Path("{id}/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response get(@PathParam("id") String id, @PathParam("key") String key, @QueryParam("format") String format) throws IOException {
        var json = "";
        if (format == null) {
            format = "";
        }
        switch (format) {
            case "vcas":
                var printer = new DefaultPrettyPrinter();
                printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
                json = new ObjectMapper().writer(printer)
                        .writeValueAsString(slideService.getSlide4Vcas(id, key));
                break;
            default:
                json = new ObjectMapper().writeValueAsString(slideService.getSlide(id, key));
        }

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
