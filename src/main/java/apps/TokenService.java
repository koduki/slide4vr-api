package apps;

import static com.google.cloud.datastore.StructuredQuery.CompositeFilter.and;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.hasAncestor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import javax.enterprise.context.Dependent;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;

/**
 *
 * @author koduki
 */
@Dependent
public class TokenService {

    public String generate(final String userId) {
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

    public String getToken(final String userId) {
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

    public String getUserId(final String token) {
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

}