/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fw;

import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

/**
 *
 * @author koduki
 */
@Provider
public class ContentTypeSetterPreProcessorInterceptor implements ContainerRequestFilter {

    private @Context
    HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        request.setAttribute(InputPart.DEFAULT_CHARSET_PROPERTY, StandardCharsets.UTF_8.name());
    }
}
