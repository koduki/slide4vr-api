/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import com.fasterxml.jackson.core.JsonProcessingException;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import dev.nklab.jl2.web.profile.Trace;
import dev.nklab.kuda.core.Trigger;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.inject.Inject;

/**
 *
 * @author koduki
 */
@Dependent
public class Pptx2pngService {

    @Inject
    Trigger trigger;

    @ConfigProperty(name = "slide4vr.gcp.projectid")
    String projectId;
    @ConfigProperty(name = "slide4vr.gcp.cloudtasks.qid")
    String queueId;
    @ConfigProperty(name = "slide4vr.gcp.cloudtasks.location")
    String locationId;

    @Trace
    public void request(String userId, String key, String contentType, String extention) {
        try {
            var pptxName = userId + "/" + key + extention;
            var pngDir = userId + "/" + key;

            var params = Map.of(
                    "userId", userId,
                    "key", key,
                    "targetParams", Map.of("args", pptxName + "," + pngDir)
            );

            var condKey = (SlideFormBean.CONTENT_TYPE_PDF.equals(contentType)) ? "pdf"
                    : (SlideFormBean.CONTENT_TYPE_PPTX.equals(contentType)) ? "pptx"
                    : null;
            trigger.callTrigger(params, condKey);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
