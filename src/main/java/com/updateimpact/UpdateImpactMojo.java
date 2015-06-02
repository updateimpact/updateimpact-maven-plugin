package com.updateimpact;

import com.google.gson.Gson;
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

import java.util.*;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "dependencies", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class UpdateImpactMojo extends AbstractMojo {
    private static final UUID BUILD_ID = UUID.randomUUID();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(required = true, property = "updateimpact.apikey")
    private String apikey;

    public void execute() throws MojoExecutionException {
        try {
            ArtifactFilter filter = new ScopeArtifactFilter("compile");

            DependencyNode rootNode = dependencyTreeBuilder.buildDependencyTree(project,
                    localRepository,
                    filter);
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

            System.out.println(new Gson().toJson(report));

        } catch (DependencyTreeBuilderException e) {
            throw new MojoExecutionException("Exception when building the dependency tree", e);
        }
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
}
