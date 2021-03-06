package fw;

import static dev.nklab.jl2.Extentions.$;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nklab.jl2.web.logging.Logger;
import java.util.List;

@ApplicationScoped
@Provider
public class JsonExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger("slide4vr");

    @Override
    public Response toResponse(Exception exception) {
        var errorMessage = (exception.getMessage() == null) ? "" : exception.getMessage();
        var msg = List.of(Map.of("error", exception.getClass().getName(), "message", errorMessage));
        if (exception instanceof AuthException) {
            return toAuthErrorResponse((AuthException) exception, msg);
        } else if (exception instanceof NotFoundResourceException) {
            return toNotFoundErrorResponse((NotFoundResourceException) exception, msg);
        } else {
            return toInternalErrorResponse(exception, msg);
        }
    }

    Response toAuthErrorResponse(AuthException exception, List<Map<String, String>> msg) {
        try {
            LOGGER.warn("auth-failed",
                    $("message", exception.getMessage()),
                    $("stacktrace", parseStackTrace(exception)));

            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ObjectMapper().writeValueAsString(msg)).build();
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    Response toNotFoundErrorResponse(NotFoundResourceException exception, List< Map<String, String>> msg) {
        try {
            LOGGER.warn("not-found",
                    $("message", exception.getMessage()),
                    $("stacktrace", parseStackTrace(exception)));

            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ObjectMapper().writeValueAsString(msg)).build();

        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    Response toInternalErrorResponse(Exception exception, List<Map<String, String>> msg) {
        try {
            LOGGER.severe(exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ObjectMapper().writeValueAsString(msg)).build();
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    String parseStackTrace(Exception exception) {
        try ( var sw = new StringWriter();  var pw = new PrintWriter(sw);) {
            exception.printStackTrace(pw);
            pw.flush();

            return sw.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
