package io.littlehorse.connector.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import io.quarkus.runtime.Startup;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

/**
 * Persistent {@link ChatMemoryStore} backed by PostgreSQL so that chat sessions survive container
 * restarts. Quarkus LangChain4j auto-detects this bean and uses it for AI services that keep memory.
 */
@Startup
@ApplicationScoped
public class PostgresChatMemoryStore implements ChatMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresChatMemoryStore.class);

    private final DataSource dataSource;

    public PostgresChatMemoryStore(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    void createTable() {
        final String ddl = "CREATE TABLE IF NOT EXISTS chat_memory ("
                + "memory_id TEXT PRIMARY KEY, "
                + "messages TEXT NOT NULL)";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(ddl)) {
            statement.execute();
        } catch (final SQLException e) {
            throw new IllegalStateException("Could not initialize the chat_memory table", e);
        }
    }

    @Override
    public List<ChatMessage> getMessages(final Object memoryId) {
        final String sql = "SELECT messages FROM chat_memory WHERE memory_id = ?";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memoryId.toString());
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return ChatMessageDeserializer.messagesFromJson(resultSet.getString(1));
                }
                return List.of();
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Could not read chat memory for " + memoryId, e);
        }
    }

    @Override
    public void updateMessages(final Object memoryId, final List<ChatMessage> messages) {
        LOG.debug("Persisting {} chat message(s) for memoryId {}", messages.size(), memoryId);
        final String sql = "INSERT INTO chat_memory (memory_id, messages) VALUES (?, ?) "
                + "ON CONFLICT (memory_id) DO UPDATE SET messages = EXCLUDED.messages";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memoryId.toString());
            statement.setString(2, ChatMessageSerializer.messagesToJson(messages));
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IllegalStateException("Could not persist chat memory for " + memoryId, e);
        }
    }

    @Override
    public void deleteMessages(final Object memoryId) {
        LOG.debug("Deleting chat memory for memoryId {}", memoryId);
        final String sql = "DELETE FROM chat_memory WHERE memory_id = ?";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memoryId.toString());
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IllegalStateException("Could not delete chat memory for " + memoryId, e);
        }
    }
}
