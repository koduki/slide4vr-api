/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import dev.nklab.jl2.web.profile.Trace;
import dev.nklab.kuda.core.Trigger;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import javax.inject.Inject;

/**
 *
 * @author koduki
 */
@Dependent
public class Transform2pngService {

    @Inject
    Trigger trigger;

    @ConfigProperty(name = "kuda.url")
    String kudaUrl;

    @Trace
    public void request(String userId, String key, String contentType, String extention, String tracecontext) {
        try {
            System.out.println("Call Flow");
            var url = kudaUrl + "/flow";
            var pptxName = userId + "/" + key + extention;
            var pngDir = userId + "/" + key;
            var condKey = (SlideFormBean.CONTENT_TYPE_PDF.equals(contentType)) ? "pdf"
                    : (SlideFormBean.CONTENT_TYPE_PPTX.equals(contentType)) ? "pptx"
                    : null;
            var params = Map.of(
                    "userId", userId,
                    "condKey", condKey,
                    "key", key,
                    "targetParams", Map.of("args", pptxName + "," + pngDir)
            );

            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(params)))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .header("X-Cloud-Trace-Context", tracecontext)
                    .uri(URI.create(url));

//       return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(System.out::println);

        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
