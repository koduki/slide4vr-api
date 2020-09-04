package apps;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import static dev.nklab.jl2.web.gcp.datastore.Extentions.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import dev.nklab.jl2.web.profile.Trace;
import java.util.UUID;
import javax.inject.Inject;

/**
 *
 * @author koduki
 */
@Dependent
public class SlideService {

    @Inject
    SlideUploader uploader;

    @ConfigProperty(name = "slide4vr.gcp.projectid")
    String projectId;
    @ConfigProperty(name = "slide4vr.gcp.bucketname.slide")
    String slideBucket;

    @Trace
    public String create(String userId, SlideFormBean slide) {
        var tz = TimeZone.getTimeZone("UTC");
        var df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

        var key = UUID.randomUUID().toString();
        uploader.upload(userId, key, slide.getSlide(), slide.getExtention());

        var datastore = DatastoreOptions.getDefaultInstance().getService();
        var slideKey = datastore.newKeyFactory()
                .addAncestors(PathElement.of("User", userId))
                .setKind("Slide")
                .newKey(key);
        var task = Entity.newBuilder(slideKey)
                .set("title", noindex(slide.getTitle()))
                .set("is_uploaded", noindex(false))
                .set("created_at", noindex(df.format(new Date())))
                .build();
        datastore.put(task);

        return key;
    }

    @Trace
    public boolean delete(String userId, String key) {
        var datastore = DatastoreOptions.getDefaultInstance().getService();
        var slideKey = datastore.newKeyFactory()
                .addAncestors(PathElement.of("User", userId))
                .setKind("Slide")
                .newKey(key);
        datastore.delete(slideKey);
        return uploader.delete(userId, key);
    }

    @Trace
    public Map<String, Map<String, List<String>>> getSlide(String userId, String key) {
        var baseUrl = "https://storage.googleapis.com";

        var storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        var bucket = storage.get(slideBucket);
        var option = Storage.BlobListOption.prefix(userId + "/" + key);

        var items = bucket.list(option).iterateAll();

        var result = (List<String>) new ArrayList<String>();
        for (var x : items) {
            result.add(baseUrl + "/" + slideBucket + "/" + x.getName());
        }
        var item = Map.of("whiteboard", Map.of("source_urls", result));

        return item;
    }

    @Trace
    public List<Map<String, Object>> listSlides(String userId) {
        var datastore = DatastoreOptions.getDefaultInstance().getService();
        //        var query = Query.newGqlQueryBuilder(Query.ResultType.ENTITY,
//                "SELECT * FROM Slide WHERE __key__ HAS ANCESTOR KEY(User, @id)")
//                .setBinding("id", id)
//                .build();
        var query = Query.newEntityQueryBuilder()
                .setKind("Slide")
                .setFilter(StructuredQuery.PropertyFilter.hasAncestor(
                        datastore.newKeyFactory().setKind("User").newKey(userId)))
                .build();
        var result = new ArrayList<Map<String, Object>>();
        var slides = datastore.run(query);
        while (slides.hasNext()) {
            var slide = slides.next();
            result.add(Map.of(
                    "key", slide.getKey().getName(),
                    "title", slide.getString("title"),
                    "is_uploaded", (slide.contains("is_uploaded")) ? slide.getBoolean("is_uploaded") : "false",
                    "created_at", slide.getString("created_at")
            ));
        }
        return result;
    }
}
