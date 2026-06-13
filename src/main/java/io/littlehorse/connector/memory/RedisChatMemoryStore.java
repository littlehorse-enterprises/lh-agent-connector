package io.littlehorse.connector.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent {@link ChatMemoryStore} backed by Redis so that chat sessions survive container
 * restarts. Quarkus LangChain4j auto-detects this bean and uses it for AI services that keep memory.
 */
@ApplicationScoped
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(RedisChatMemoryStore.class);
    private static final String KEY_PREFIX = "chat-memory:";

    private final ValueCommands<String, String> commands;

    public RedisChatMemoryStore(final RedisDataSource redis) {
        this.commands = redis.value(String.class, String.class);
    }

    @Override
    public List<ChatMessage> getMessages(final Object memoryId) {
        final String json = commands.get(key(memoryId));
        if (json == null) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(final Object memoryId, final List<ChatMessage> messages) {
        LOG.debug("Persisting {} chat message(s) for memoryId {}", messages.size(), memoryId);
        commands.set(key(memoryId), ChatMessageSerializer.messagesToJson(messages));
    }

    @Override
    public void deleteMessages(final Object memoryId) {
        LOG.debug("Deleting chat memory for memoryId {}", memoryId);
        commands.getdel(key(memoryId));
    }

    private static String key(final Object memoryId) {
        return KEY_PREFIX + memoryId;
    }
}
