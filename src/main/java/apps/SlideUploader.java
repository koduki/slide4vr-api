/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.StorageOptions;
import dev.nklab.jl2.web.profile.Trace;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 *
 * @author koduki
 */
@Dependent
public class SlideUploader {

    @ConfigProperty(name = "slide4vr.gcp.projectid")
    String projectId;
    @ConfigProperty(name = "slide4vr.gcp.bucketname.pptx")
    String pptxBucket;
    @ConfigProperty(name = "slide4vr.gcp.bucketname.slide")
    String slideBucket;

    @Trace
    public void upload(String userId, String key, byte[] data, String extention) {
        var storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        var blobId = BlobId.of(pptxBucket, userId + "/" + key + extention);
        var blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, data);
    }

    @Trace
    public boolean delete(String userId, String key) {
        var storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        var bucket = storage.get(slideBucket);
        var blobIds = StreamSupport.stream(bucket.list().iterateAll().spliterator(), false)
                .filter(x -> x.getName().startsWith(userId + "/" + key))
                .map(x -> x.getBlobId())
                .collect(Collectors.toList());

        return storage.delete(blobIds).stream().allMatch(x -> x);
    }
}
