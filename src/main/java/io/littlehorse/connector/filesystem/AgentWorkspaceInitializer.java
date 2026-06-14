package io.littlehorse.connector.filesystem;

import io.quarkus.runtime.Startup;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ensures the filesystem agent's workspace directory exists before the MCP filesystem server starts.
 *
 * <p>The {@code @modelcontextprotocol/server-filesystem} process exits immediately with {@code ENOENT}
 * if its allowed directory is missing, which surfaces as a transport failure when langchain4j tries to
 * retrieve the MCP tools. Creating the directory eagerly at startup avoids that race.
 */
@Startup
@ApplicationScoped
public class AgentWorkspaceInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(AgentWorkspaceInitializer.class);

    private final String workspace;

    public AgentWorkspaceInitializer(
            @ConfigProperty(name = "lhc.agent.workspace") final String workspace) {
        this.workspace = workspace;
    }

    @PostConstruct
    void createWorkspace() {
        final Path path = Path.of(workspace);
        try {
            Files.createDirectories(path);
            LOG.info("Filesystem agent workspace ready at {}", path);
        } catch (final IOException e) {
            throw new IllegalStateException(
                    "Could not create filesystem agent workspace at " + path, e);
        }
    }
}
