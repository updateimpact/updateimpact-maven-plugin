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
    private static final List<ModuleDependencies> moduleDependencies = new ArrayList<ModuleDependencies>();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(property = "updateimpact.apikey")
    private String apikey;

    @Parameter(required = true, property = "updateimpact.url", defaultValue = "https://app.updateimpact.com")
    private String url;

    @Parameter(required = true, property = "updateimpact.openbrowser", defaultValue = "true")
    private boolean openBrowser;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    public void execute() throws MojoExecutionException {
        setAndVerifyApiKey();

        ArtifactFilter filter = new ScopeArtifactFilter("compile");
        DependencyNode rootNode;

        try {
            rootNode = dependencyTreeBuilder.buildDependencyTree(project, localRepository, filter);
        } catch (DependencyTreeBuilderException e) {
            throw new MojoExecutionException("Exception when building the dependency tree", e);
        }

        ModuleDependencies md = createModuleDependencies(rootNode);
        moduleDependencies.add(md);

        submitIfLastProject();
    }

    private ModuleDependencies createModuleDependencies(DependencyNode rootNode) {
        final DependencyId rootNodeId = dependencyIdFromNode(rootNode);

        final Map<DependencyId, Dependency> allDependencies = new HashMap<DependencyId, Dependency>();

        rootNode.accept(new DependencyNodeVisitor() {
            public boolean visit(DependencyNode node) {
                List<DependencyId> children = new ArrayList<DependencyId>();
                System.out.println("NODE: " + node.getArtifact());
                for (DependencyNode childNode : node.getChildren()) {
                    System.out.println("  CHILD " + childNode.getArtifact());
                    children.add(dependencyIdFromNode(childNode));
                }

                DependencyId newDependencyId = dependencyIdFromNode(node);

                Dependency newDependency = new Dependency(
                        newDependencyId,
                        node.getState() == DependencyNode.OMITTED_FOR_CONFLICT ? node.getRelatedArtifact().getVersion() : null,
                        node.getState() == DependencyNode.OMITTED_FOR_CYCLE ? true : null,
                        children.size() > 0 ? children : null);

                if (allDependencies.containsKey(newDependencyId)) {
                    Dependency current = allDependencies.get(newDependencyId);
                    allDependencies.put(newDependencyId, mergeDependencies(newDependency, current));
                } else {
                    allDependencies.put(newDependencyId, newDependency);
                }

                return true;
            }

            public boolean endVisit(DependencyNode node) {
                return true;
            }
        });

        return new ModuleDependencies(rootNodeId, "test", allDependencies.values());
    }

    private void submitIfLastProject() throws MojoExecutionException {
        if (isLastProject()) {
            DependencyReport dr = new DependencyReport(
                    getProjectName(),
                    apikey,
                    buildId(),
                    moduleDependencies,
                    Collections.<ModuleIvyReport>emptyList(),
                    "1.0",
                    "maven-plugin-1.0.9");

            SubmitLogger log = new SubmitLogger() {
                public void info(String message) { getLog().info(message); }
                public void error(String message) { getLog().error(message); }
            };

            String link = new ReportSubmitter(url, log).trySubmitReport(dr);
            if (link != null) {
                if (openBrowser) {
                    getLog().info("Trying to open the report in the default browser ... " +
                            "(you can disable this by setting the updateimpact.openbrowser property to false)");

                    try {
                        openWebpage(link);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Exception when trying to open a link in the default browser", e);
                    }
                }
            }

            moduleDependencies.clear();
        }
    }

    private String buildId() {
        // configurable via env, from git commit id?
        return BUILD_ID.toString();
    }

    private boolean isLastProject() {
        return project == reactorProjects.get(reactorProjects.size() - 1);
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

    private void setAndVerifyApiKey() throws MojoExecutionException {
        if (apikey == null || "".equals(apikey)) {
            apikey = System.getenv("UPDATEIMPACT_API_KEY");
        }

        if (apikey == null || "".equals(apikey)) {
            throw new MojoExecutionException("Please define the api key. You can find it on UpdateImpact.com");
        }
    }

    private Dependency mergeDependencies(Dependency d1, Dependency d2) {
        LinkedHashSet<DependencyId> allChildren = new LinkedHashSet<DependencyId>();
        if (d1.getChildren() != null) allChildren.addAll(d1.getChildren());
        if (d2.getChildren() != null) allChildren.addAll(d2.getChildren());
        return new Dependency(
                d1.getId(),
                d1.getEvictedByVersion() == null ? d2.getEvictedByVersion() : d1.getEvictedByVersion(),
                d1.getCycle() == null ? d2.getCycle() : d1.getCycle(),
                new ArrayList<DependencyId>(allChildren)
        );
    }
}
