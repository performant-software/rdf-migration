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
 * Access to the infrastructure of the Advanced Research Consortium (ARC).
 *
 * <p>ARC's GitLab service can be queried via its API.</p>
 */
public class Arc {

    /**
     * A project in ARC's GitLab service.
     */
    public static class GitLabProject {
        /**
         * The full path of the project, including the namespace.
         */
        @Json(name = "path_with_namespace")
        public final String path;

        /**
         * The name of the project.
         */
        @Json(name = "path")
        public final String name;

        public GitLabProject(String path, String name) {
            this.path = path;
            this.name = name;
        }

        /**
         * Constructs a Git URL for cloning the repository of this project.
         * @return a Git URL, e.g. <code>git@gitlab.tamu.edu:test/project.git</code>
         */
        public String gitUrl() {
            return String.join(":",
                String.join("@", "git", GIT_LAB_HOST),
                String.join(".", path, "git")
            );
        }

        /**
         * Constructs a GitLab frontend URL for this project.
         * @return a frontend URL, e.g. <code>https://gitlab.tamu.edu/test/project/</code>
         */
        public HttpUrl url() {
            return new HttpUrl.Builder()
                .scheme("https").host(GIT_LAB_HOST)
                .addPathSegments(path)
                .build();
        }

        /**
         * Constructs a GitLab frontend URL for a file in this project.
         *
         * @param branch the branch to address
         * @param path the path of the addressed file
         * @return a frontend URL
         */
        public HttpUrl url(String branch, String path) {
            return url().newBuilder()
                .addPathSegment("tree").addPathSegment(branch)
                .addPathSegments(path)
                .build();
        }

        /**
         * Constructs a GitLab frontend URL for a line in a file of this project.
         *
         * @param branch the branch to address
         * @param path  the path of the addressed file
         * @param lineNumber the line number
         * @return a frontend URL
         */
        public HttpUrl url(String branch, String path, int lineNumber) {
            return url(branch, path).newBuilder()
                .fragment(String.format("L%s", lineNumber))
                .build();
        }

        @Override
        public String toString() {
            return url().toString();
        }
    }

    public static final String GIT_LAB_HOST = "gitlab.tamu.edu";

    private final Logger logger = Logging.forClass(Arc.class);
    private final Moshi moshi = new Moshi.Builder().build();

    private final OkHttpClient httpClient;
    private final HttpUrl apiUrl;
    private final String gitLabToken;

    /**
     * Creates an instance of ARC's infrastructure.
     *
     * <p>The private token required for accessing GitLab's API is read from the environment
     * variable <code>GITLAB_PRIVATE_TOKEN</code>.</p>
     *
     * @throws NullPointerException if the environment variable is not defined
     */
    public Arc() {
        this(Objects.requireNonNull(
                System.getenv("GITLAB_PRIVATE_TOKEN"),
                "$GITLAB_PRIVATE_TOKEN"
        ));
    }

    /**
     * Creates an instance of ARC's infrastructure.
     *
     * @param gitLabToken the private token to authenticate requests against GitLab's API
     */
    public Arc(String gitLabToken) {
        final OkHttpClient.Builder httpClientBuilder = Util.trustfulHttpClient(
            new OkHttpClient.Builder()
        );
        if (logger.isLoggable(Level.FINE)) {
            httpClientBuilder.addInterceptor(
                new HttpLoggingInterceptor(logger::fine).setLevel(BODY)
            );
        }
        this.httpClient = httpClientBuilder.build();
        this.apiUrl = new HttpUrl.Builder()
                .scheme("https").host(GIT_LAB_HOST)
                .addEncodedPathSegments("api/v3/").build();
        this.gitLabToken = gitLabToken;
    }

    /**
     * Retrieves all accessible projects containing ARC's RDF source material.
     *
     * @return an array of GitLab projects
     * @throws IOException in case of a network error
     */
    public GitLabProject[] rdfRepositories() throws IOException {
        return gitLabProjects().stream()
                .filter(gp -> gp.name.startsWith("arc_rdf"))
                .sorted(Comparator.comparing((GitLabProject gp) -> gp.path))
                .toArray(GitLabProject[]::new);
    }

    /**
     * Returns all accessible projects in ARC's GitLab service.
     *
     * @return an array of all GitLab projects
     * @throws IOException in case of a network error
     */
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

    private <T> List<T> gitLabPages(Function<HttpUrl.Builder, HttpUrl.Builder> url,
                                    Class<T> type, int pageSize, int maxPages) throws IOException {
        final JsonAdapter<List<T>> jsonAdapter = moshi.adapter(
            Types.newParameterizedType(List.class, type)
        );

        pageSize = Math.max(1, Math.min(100, pageSize));
        final List<T> result = new LinkedList<>();
        for (int page = 1; page <= maxPages; page++) {
            final Function<HttpUrl.Builder, HttpUrl.Builder> paginatedUrl = url
                .andThen(gitLabPaginate(page, pageSize));
            final Request request = gitLabRequest(paginatedUrl).build();
            final Response response = httpClient.newCall(request).execute();
            final List<T> pageContents = jsonAdapter.fromJson(response.body().source());
            result.addAll(pageContents);
            if (pageContents.size() < pageSize) {
                break;
            }
        }
        return result;
    }

    private static Function<HttpUrl.Builder, HttpUrl.Builder> gitLabPaginate(int page,
                                                                             int pageSize) {
        return url -> url
                .addQueryParameter("page", Integer.toString(page))
                .addQueryParameter("per_page", Integer.toString(pageSize));
    }
}
