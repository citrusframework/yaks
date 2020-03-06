package dev.yaks.testing.http;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.message.MessageType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 */
public interface HttpSteps {

    /**
     * Maps content type value to Citrus message type used later on for selecting
     * the right message validator implementation.
     *
     * @param contentType
     * @return
     */
    default String getMessageType(String contentType) {
        List<MediaType> binaryMediaTypes = Arrays.asList(MediaType.APPLICATION_OCTET_STREAM,
                MediaType.APPLICATION_PDF,
                MediaType.IMAGE_GIF,
                MediaType.IMAGE_JPEG,
                MediaType.IMAGE_PNG,
                MediaType.valueOf("application/zip"));

        if (contentType.equals(MediaType.APPLICATION_JSON_VALUE) ||
                contentType.equals(MediaType.APPLICATION_JSON_UTF8_VALUE)) {
            return MessageType.JSON.name();
        } else if (contentType.equals(MediaType.APPLICATION_XML_VALUE)) {
            return MessageType.XML.name();
        } else if (contentType.equals(MediaType.APPLICATION_XHTML_XML_VALUE)) {
            return MessageType.XHTML.name();
        } else if (contentType.equals(MediaType.TEXT_PLAIN_VALUE) ||
                contentType.equals(MediaType.TEXT_HTML_VALUE)) {
            return MessageType.PLAINTEXT.name();
        } else if (binaryMediaTypes.stream().anyMatch(mediaType -> contentType.equals(mediaType.getType()))) {
            return MessageType.BINARY.name();
        }

        return Citrus.DEFAULT_MESSAGE_TYPE;
    }

    /**
     * Prepare request message with given body, headers, method and path.
     * @param body
     * @param headers
     * @param method
     * @param path
     * @return
     */
    default HttpMessage createRequest(String body, Map<String, String> headers, Map<String, String> params, String method, String path) {
        HttpMessage request = new HttpMessage();
        request.method(HttpMethod.valueOf(method));

        if (StringUtils.hasText(path)) {
            request.path(path);
            request.contextPath(path);
        }

        if (StringUtils.hasText(body)) {
            request.setPayload(body);
        }

        for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
            request.setHeader(headerEntry.getKey(), headerEntry.getValue());
        }

        for (Map.Entry<String, String> paramEntry : params.entrySet()) {
            request.queryParam(paramEntry.getKey(), paramEntry.getValue());
        }

        return request;
    }

    /**
     * Prepare response message with given body, headers and status.
     * @param body
     * @param headers
     * @param status
     * @return
     */
    default HttpMessage createResponse(String body, Map<String, String> headers, Integer status) {
        HttpMessage response = new HttpMessage();
        response.status(HttpStatus.valueOf(status));

        if (StringUtils.hasText(body)) {
            response.setPayload(body);
        }

        for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
            response.setHeader(headerEntry.getKey(), headerEntry.getValue());
        }

        return response;
    }
}
