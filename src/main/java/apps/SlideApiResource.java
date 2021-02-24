package apps;

import static dev.nklab.jl2.Extentions.*;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpHeaders;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.*;
import static com.google.cloud.datastore.StructuredQuery.CompositeFilter.*;

import io.quarkus.security.Authenticated;
import java.io.IOException;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import dev.nklab.jl2.web.profile.WebTrace;
import dev.nklab.jl2.web.logging.Logger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Date;
import javax.ws.rs.DELETE;
import javax.ws.rs.QueryParam;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/api/slide")
public class SlideApiResource {

    private final Logger logger = Logger.getLogger("slide4vr");

    @Inject
    SlideService slideService;

    @Inject
    Pptx2pngService pptx2pngService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response list(@HeaderParam("x-slide4vr-auth") String token) throws IOException, ParseException {
        // var userId = "0m3ItnvCMQhbACV9rR5mkdmFOns2";

        // System.out.println(token2);
        // var token = generate(userId);

        var userId = getUserId(token);
        System.out.println(userId);
        System.out.println(getToken(userId));
 
        logger.debug("getList", $("id", userId));

        var slides = slideService.listSlides(userId);
        return Response.ok(new ObjectMapper().writeValueAsString(slides))
        .build();
    }

    private String generate(final String userId) {
        final var tz = TimeZone.getTimeZone("UTC");
        final var df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

        final var datastore = DatastoreOptions.getDefaultInstance().getService();

        var query = Query.newEntityQueryBuilder().setKind("ApplicationToken").setFilter(
                and(hasAncestor(datastore.newKeyFactory().setKind("User").newKey(userId)), eq("is_enable", true)))
                .build();
        var rs = datastore.run(query);
        while (rs.hasNext()) {
            var e = Entity.newBuilder(rs.next()).set("is_enable", false).set("updated_at", df.format(new Date()))
                    .build();
            datastore.update(e);
        }

        var token = UUID.randomUUID().toString();
        var tokenKey = datastore.newKeyFactory().addAncestors(PathElement.of("User", userId))
                .setKind("ApplicationToken").newKey();
        var entity = Entity.newBuilder(tokenKey).set("token", token).set("is_enable", true)
                .set("created_at", df.format(new Date())).set("updated_at", df.format(new Date())).build();
        datastore.put(entity);

        return token;
    }

    private String getToken(final String userId) {
        var datastore = DatastoreOptions.getDefaultInstance().getService();

        var query = Query.newEntityQueryBuilder().setKind("ApplicationToken").setFilter(
                and(hasAncestor(datastore.newKeyFactory().setKind("User").newKey(userId)), eq("is_enable", true)))
                .build();
        var rs = datastore.run(query);
        var result = "";
        while (rs.hasNext()) {
            result = rs.next().getString("token");
        }
        return result;
    }

    private String getUserId(final String token) {
        var datastore = DatastoreOptions.getDefaultInstance().getService();
        System.out.println("query: " + token);
        var query = Query.newEntityQueryBuilder().setKind("ApplicationToken").setFilter(eq("token", token)).build();
        var rs = datastore.run(query);
        var result = "";
        while (rs.hasNext()) {
            result = rs.next().getKey().getAncestors().get(0).getName();
        }

        return result;
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
    @Authenticated
    public Response create(@Context final SecurityContext ctx, final MultipartFormDataInput form) throws IOException {
        final var slide = new SlideFormBean(form.getFormDataMap());

        final var id = ctx.getUserPrincipal().getName();
        final var key = slideService.create(id, slide);
        pptx2pngService.request(id, key, slide.getContentType(), slide.getExtention());

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
