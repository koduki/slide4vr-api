/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

/**
 *
 * @author koduki
 */
public class SlideFormBean {

    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private String filename = null;

    private String contentType = null;

    private String extention = null;

    private String title = null;

    private byte[] slide = null;

    public SlideFormBean() {
    }

    public SlideFormBean(Map<String, List<InputPart>> form) throws IOException {
        this.title = form.get("title").get(0).getBodyAsString();

        var data = form.get("slide").get(0);
        this.filename = getFileName(data.getHeaders());
        this.contentType = data.getHeaders().getFirst("Content-Type");
        this.extention = getExtention(contentType);
        this.slide = data.getBody(InputStream.class, null).readAllBytes();
    }

    private String getFileName(MultivaluedMap<String, String> header) {
        var content = header.getFirst("Content-Disposition").split(";");
        for (var filename : content) {
            if ((filename.trim().startsWith("filename"))) {
                var name = filename.split("=");
                var finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }

    private String getExtention(String contentType) throws RuntimeException {
        String ext;
        switch (contentType) {
            case SlideFormBean.CONTENT_TYPE_PDF:
                ext = ".pdf";
                break;
            case SlideFormBean.CONTENT_TYPE_PPTX:
                ext = ".pptx";
                break;
            default:
                throw new RuntimeException("unkown format: " + contentType);
        }
        return ext;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getExtention() {
        return extention;
    }

    public void setExtention(String extention) {
        this.extention = extention;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public byte[] getSlide() {
        return slide;
    }

    public void setSlide(byte[] slide) {
        this.slide = slide;
    }

    @Override
    public String toString() {
        return "SlideFormBean{" + "filename=" + filename + ", contentType=" + contentType + ", title=" + title + ", slide=" + ((slide == null) ? "empty" : "data") + '}';
    }

}
