/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.swagger;

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
