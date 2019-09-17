package dev.yaks.testing.swagger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import com.consol.citrus.util.FileUtils;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContexts;

/**
 * Loads Swagger Open API specifications from different locations like file resource or web resource.
 * @author Christoph Deppisch
 */
public final class SwaggerResourceLoader {

    /**
     * Prevent instantiation of utility class.
     */
    private SwaggerResourceLoader() {
        super();
    }

    /**
     * Loads the specification from a file resource. Either classpath or file system resource path is supported.
     * @param resource
     * @return
     */
    public static Swagger fromFile(String resource) {
        try {
            return new SwaggerParser().parse(FileUtils.readToString(FileUtils.getFileResource(resource)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Swagger Open API specification: " + resource, e);
        }
    }

    /**
     * Loads specification from given web URL location.
     * @param url
     * @return
     */
    public static Swagger fromWebResource(URL url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            if (status > 299) {
                throw new IllegalStateException("Failed to retrieve Swagger Open API specification: " + url.toString(),
                        new IOException(FileUtils.readToString(con.getErrorStream())));
            } else {
                return new SwaggerParser().parse(FileUtils.readToString(con.getInputStream()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve Swagger Open API specification: " + url.toString(), e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
     * Loads specification from given web URL location using secured Http connection.
     * @param url
     * @return
     */
    public static Swagger fromSecuredWebResource(URL url) {
        Objects.requireNonNull(url);

        HttpsURLConnection con = null;
        try {
            SSLContext sslcontext = SSLContexts
                    .custom()
                    .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                    .build();

            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(NoopHostnameVerifier.INSTANCE);

            con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            if (status > 299) {
                throw new IllegalStateException("Failed to retrieve Swagger Open API specification: " + url.toString(),
                        new IOException(FileUtils.readToString(con.getErrorStream())));
            } else {
                return new SwaggerParser().parse(FileUtils.readToString(con.getInputStream()));
            }
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new IllegalStateException("Failed to create https client for ssl connection", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve Swagger Open API specification: " + url.toString(), e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
