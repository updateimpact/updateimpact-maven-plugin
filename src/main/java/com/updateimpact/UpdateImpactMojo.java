package com.updateimpact;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;

@Mojo(name = "dependencies", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class UpdateImpactMojo extends AbstractMojo {
    // these are static so that they are shared across multi-module builds
    private static final UUID BUILD_ID = UUID.randomUUID();
    private static final HttpClient HTTP_CLIENT = new DefaultHttpClient();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(required = true, property = "updateimpact.apikey")
    private String apikey;

    @Parameter(required = true, property = "updateimpact.url", defaultValue = "https://updateimpact.com")
    private String url;

    @Parameter(required = true, property = "updateimpact.openbrowser", defaultValue = "true")
    private boolean openBrowser;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    public void execute() throws MojoExecutionException {
        ArtifactFilter filter = new ScopeArtifactFilter("compile");
        DependencyNode rootNode;

        try {
            rootNode = dependencyTreeBuilder.buildDependencyTree(project, localRepository, filter);
        } catch (DependencyTreeBuilderException e) {
            throw new MojoExecutionException("Exception when building the dependency tree", e);
        }

        final DependencyId rootNodeId = idFromNode(rootNode);

        final Map<DependencyId, Dependency> allDependencies = new HashMap<DependencyId, Dependency>();

        rootNode.accept(new DependencyNodeVisitor() {
            public boolean visit(DependencyNode node) {
                if (node.getState() == DependencyNode.INCLUDED) {
                    List<DependencyChild> children = new ArrayList<DependencyChild>();
                    for (DependencyNode childNode : node.getChildren()) {
                        children.add(childFromNode(childNode));
                    }

                    DependencyId newDependencyId = idFromNode(node);
                    if (allDependencies.containsKey(newDependencyId)) {
                        getLog().warn("Duplicate dependency: " + node);
                    } else {
                        allDependencies.put(newDependencyId, new Dependency(
                                newDependencyId,
                                children.size() > 0 ? children : null));
                    }
                }

                return true;
            }

            public boolean endVisit(DependencyNode node) {
                return true;
            }
        });

        DependencyReport report = new DependencyReport(apikey, buildId(),
                Collections.singletonList(new DependencyTree(rootNodeId, allDependencies.values())));

        trySendReport(new Gson().toJson(report));
    }

    private DependencyId idFromNode(DependencyNode node) {
        return new DependencyId(
                node.getArtifact().getGroupId(),
                node.getArtifact().getArtifactId(),
                node.getArtifact().getVersion(),
                node.getArtifact().getType(),
                node.getArtifact().getClassifier()
        );
    }

    private DependencyChild childFromNode(DependencyNode node) {
        return new DependencyChild(
                idFromNode(node),
                node.getState() == DependencyNode.OMITTED_FOR_CONFLICT ? node.getRelatedArtifact().getVersion() : null,
                node.getState() == DependencyNode.OMITTED_FOR_CYCLE ? true : null
        );
    }

    private String buildId() {
        // configurable via env, from git commit id?
        return BUILD_ID.toString();
    }

    private void trySendReport(String report) throws MojoExecutionException {
        try {
            sendReport(report);
        } catch (IOException e) {
            throw new MojoExecutionException("Exception when submitting the dependency report", e);
        }
    }

    private void sendReport(String report) throws IOException {
        String submitUrl = url + "/rest/submit";

        Log log = getLog();

        log.info("");
        log.info("Submitting dependency report to " + submitUrl);
        HttpPost post = new HttpPost(submitUrl);
        post.setEntity(new StringEntity(report));
        HttpResponse response = HTTP_CLIENT.execute(post);

        String responseJson = EntityUtils.toString(response.getEntity());
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode < 200 || statusCode > 300) {
            log.error("Cannot submit report to " + submitUrl + ", got response " + statusCode +
                    ": " + responseJson);
        } else {
            SubmitResponse submitResponse = new Gson().fromJson(responseJson, SubmitResponse.class);
            String viewLink = url + "/#/builds/" + submitResponse.getUserIdStr() + "/" + submitResponse.getBuildId();

            log.info("");
            log.info("Dependency report submitted. You can view it at: ");
            log.info(viewLink);
            log.info("");

            if (openBrowser) {
                log.info("Trying to open the report in the default browser ... " +
                        "(you can disable this by setting the updateimpact.openbrowser property to false)");
                openLinkIfLastProject(viewLink);
            }
        }
    }

    private void openLinkIfLastProject(String viewLink) throws IOException {
        if (project == reactorProjects.get(reactorProjects.size() - 1)) {
            openWebpage(viewLink);
        }
    }

    // http://stackoverflow.com/questions/10967451/open-a-link-in-browser-with-java-button
    private void openWebpage(String url) throws IOException {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI.create(url));
        }
    }
}
