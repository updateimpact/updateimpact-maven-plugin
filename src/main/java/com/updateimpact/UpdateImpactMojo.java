package com.updateimpact;

import com.updateimpact.report.*;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

@Mojo(name = "submit", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class UpdateImpactMojo extends AbstractMojo {
    // these are static so that they are shared across multi-module builds
    private static final UUID BUILD_ID = UUID.randomUUID();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(required = true, property = "updateimpact.apikey")
    private String apikey;

    @Parameter(required = true, property = "updateimpact.url", defaultValue = "https://app.updateimpact.com")
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

        DependencyReport report = createReport(rootNode);

        SubmitLogger log = new SubmitLogger() {
            public void info(String message) { getLog().info(message); }
            public void error(String message) { getLog().error(message); }
        };

        String link = new ReportSubmitter(url, log).trySubmitReport(report);
        if (link != null) {
            if (openBrowser) {
                getLog().info("Trying to open the report in the default browser ... " +
                        "(you can disable this by setting the updateimpact.openbrowser property to false)");
                openLinkIfLastProject(link);
            }
        }
    }

    private DependencyReport createReport(DependencyNode rootNode) {
        final DependencyId rootNodeId = dependencyIdFromNode(rootNode);

        final Map<DependencyId, Dependency> allDependencies = new HashMap<DependencyId, Dependency>();

        rootNode.accept(new DependencyNodeVisitor() {
            public boolean visit(DependencyNode node) {
                if (node.getState() == DependencyNode.INCLUDED) {
                    List<DependencyChild> children = new ArrayList<DependencyChild>();
                    for (DependencyNode childNode : node.getChildren()) {
                        children.add(dependencyChildFromNode(childNode));
                    }

                    DependencyId newDependencyId = dependencyIdFromNode(node);
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

        return new DependencyReport(
                getProjectName(),
                apikey,
                buildId(),
                Collections.singletonList(new ModuleDependencies(rootNodeId, "test", allDependencies.values())),
                Collections.<ModuleIvyReport>emptyList(),
                "1.0",
                "maven-plugin-1.0.7");
    }

    private String buildId() {
        // configurable via env, from git commit id?
        return BUILD_ID.toString();
    }

    private void openLinkIfLastProject(String viewLink) throws MojoExecutionException {
        if (project == reactorProjects.get(reactorProjects.size() - 1)) {
            try {
                openWebpage(viewLink);
            } catch (IOException e) {
                throw new MojoExecutionException("Exception when trying to open a link in the default browser", e);
            }
        }
    }

    // http://stackoverflow.com/questions/10967451/open-a-link-in-browser-with-java-button
    private void openWebpage(String url) throws IOException {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI.create(url));
        }
    }

    private String getProjectName() {
        MavenProject mainProject = project;
        for (MavenProject reactorProject : reactorProjects) {
            if (reactorProject.isExecutionRoot()) mainProject = reactorProject;
        }

        return mainProject.getName();
    }

    private DependencyId dependencyIdFromNode(DependencyNode node) {
        return new DependencyId(
                node.getArtifact().getGroupId(),
                node.getArtifact().getArtifactId(),
                node.getArtifact().getVersion(),
                node.getArtifact().getType(),
                node.getArtifact().getClassifier()
        );
    }

    private DependencyChild dependencyChildFromNode(DependencyNode node) {
        return new DependencyChild(
                dependencyIdFromNode(node),
                node.getState() == DependencyNode.OMITTED_FOR_CONFLICT ? node.getRelatedArtifact().getVersion() : null,
                node.getState() == DependencyNode.OMITTED_FOR_CYCLE ? true : null
        );
    }
}
