package apps;

import static dev.nklab.jl2.Extentions.$;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.StorageOptions;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.nklab.jl2.web.logging.Logger;
import dev.nklab.jl2.web.profile.WebTrace;

@Path("/healthcheck")
public class HealthcheckResource {

    private final Logger logger = Logger.getLogger("slide4vr");

    @ConfigProperty(name = "slide4vr.gcp.projectid")
    String projectId;
    @ConfigProperty(name = "slide4vr.gcp.bucketname.pptx")
    String pptxBucket;
    @ConfigProperty(name = "slide4vr.gcp.bucketname.slide")
    String slideBucket;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response get() throws IOException, ParseException {
        logger.debug("healthcheck", $("status", "start"));

        var storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        var bucket = storage.get(pptxBucket);
        var blobs = bucket.list();

        var i = 0;
        for (var blob : blobs.iterateAll()) {
            i++;
        }

        return Response.ok(new ObjectMapper().writeValueAsString(Map.of("image count", i))).build();
    }

}
