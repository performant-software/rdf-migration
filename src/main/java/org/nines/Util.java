/*
 * Copyright © 2017 The Advanced Research Consortium - ARC (http://idhmcmain.tamu.edu/arcgrant/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nines;

import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility methods.
 */
public class Util {

    /**
     * Joins non-empty strings given as an array.
     */
    public static String join(String delimiter, String... parts) {
        return join(delimiter, Stream.of(parts));
    }

    /**
     * Joins non-empty strings given as a list..
     */
    public static String join(String delimiter, List<String> parts) {
        return join(delimiter, parts.stream());
    }

    /**
     * Joins non-empty strings given as a stream.
     */

    public static String join(String delimiter, Stream<String> parts) {
        return parts
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Configures a HTTP client to trust any SSL/TLS peer.
     *
     * @param clientBuilder the builder via which the configuration is applied
     * @return the configured builder
     */
    public static OkHttpClient.Builder trustfulHttpClient(OkHttpClient.Builder clientBuilder) {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{TRUST_ALL_MANAGER}, null);
            return clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), TRUST_ALL_MANAGER);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }

    };

    /**
     * Deletes a directory recursively.
     *
     * @param directory the directory to delete
     */
    public static void deleteRecursively(Path directory) throws IOException {
        Files.walkFileTree(directory, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Ensures a given directory exists by optionally creating it.
     *
     * @param directory the directory
     * @return the existing directory
     * @throws IllegalStateException in case the directory cannot be created
     */
    public static File existingDirectory(File directory) {
        if (!directory.isDirectory() &&  !directory.mkdirs()) {
            throw new IllegalStateException(directory.toString());
        }
        return directory;
    }
}
