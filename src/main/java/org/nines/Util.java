package org.nines;

import okhttp3.OkHttpClient;

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
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class Util {

    public static String join(String delimiter, String... parts) {
        return join(delimiter, Stream.of(parts));
    }

    public static String join(String delimiter, List<String> parts) {
        return join(delimiter, parts.stream());
    }

    public static String join(String delimiter, Stream<String> parts) {
        return parts
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(delimiter));
    }

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
    public static void deleteRecursively(Path directory) throws IOException {
        Files.walkFileTree(directory, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
