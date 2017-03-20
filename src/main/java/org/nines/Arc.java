package org.nines;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class Arc {

    public static class GitLabProject {
        @Json(name = "path_with_namespace")
        public final String path;

        @Json(name = "path")
        public final String name;

        public GitLabProject(String path, String name) {
            this.path = path;
            this.name = name;
        }

        public String gitUrl() {
            return String.join(":", String.join("@", "git", GIT_LAB_HOST), String.join(".", path, "git"));
        }
        
        @Override
        public String toString() {
            return new HttpUrl.Builder()
                    .scheme("https").host(GIT_LAB_HOST)
                    .addPathSegments(path)
                    .build().toString();
        }
    }

    public static final String GIT_LAB_HOST = "gitlab.tamu.edu";

    private final Logger logger = Logging.forClass(Arc.class);
    private final Moshi moshi = new Moshi.Builder().build();

    private final OkHttpClient httpClient;
    private final HttpUrl apiUrl;
    private final String gitLabToken;

    public Arc() {
        this(Objects.requireNonNull(
                System.getenv("GITLAB_PRIVATE_TOKEN"),
                "$GITLAB_PRIVATE_TOKEN"
        ));
    }

    public Arc(String gitLabToken) {
        final OkHttpClient.Builder httpClientBuilder = Util.trustfulHttpClient(new OkHttpClient.Builder());
        if (logger.isLoggable(Level.FINE)) {
            httpClientBuilder.addInterceptor(new HttpLoggingInterceptor(logger::fine).setLevel(BODY));
        }
        this.httpClient = httpClientBuilder.build();
        this.apiUrl = new HttpUrl.Builder()
                .scheme("https").host(GIT_LAB_HOST)
                .addEncodedPathSegments("api/v3/").build();
        this.gitLabToken = gitLabToken;
    }

    public GitLabProject[] rdfRepositories() throws IOException {
        return gitLabProjects().stream()
                .filter(gp -> gp.name.startsWith("arc_rdf"))
                .sorted(Comparator.comparing((GitLabProject gp) -> gp.path).reversed())
                .toArray(GitLabProject[]::new);
    }

    public List<GitLabProject> gitLabProjects() throws IOException {
        return gitLabPages(
                url -> url.addPathSegment("projects"),
                GitLabProject.class, 100, 25
        );
    }

    private Request.Builder gitLabRequest(Function<HttpUrl.Builder, HttpUrl.Builder> url) {
        return new Request.Builder()
                .url(url.apply(apiUrl.newBuilder()).build())
                .addHeader("PRIVATE-TOKEN", gitLabToken);
    }

    private <T> List<T> gitLabPages(Function<HttpUrl.Builder, HttpUrl.Builder> url, Class<T> type, int pageSize, int maxPages) throws IOException {
        final JsonAdapter<List<T>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, type));

        pageSize = Math.max(1, Math.min(100, pageSize));
        final List<T> result = new LinkedList<>();
        for (int page = 1; page <= maxPages; page++) {
            final Request request = gitLabRequest(url.andThen(gitLabPaginate(page, pageSize))).build();
            final Response response = httpClient.newCall(request).execute();
            final List<T> pageContents = jsonAdapter.fromJson(response.body().source());
            result.addAll(pageContents);
            if (pageContents.size() < pageSize) {
                break;
            }
        }
        return result;
    }

    private static Function<HttpUrl.Builder, HttpUrl.Builder> gitLabPaginate(int page, int pageSize) {
        return url -> url
                .addQueryParameter("page", Integer.toString(page))
                .addQueryParameter("per_page", Integer.toString(pageSize));
    }
}
