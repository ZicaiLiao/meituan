package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.ConversationScene;
import com.meituan.demo.backend.model.DomainModels.Message;
import com.meituan.demo.backend.model.DomainModels.MessageType;
import com.meituan.demo.backend.model.DomainModels.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Conversation> findConversationsByUserId(Long userId) {
        List<Conversation> conversations = jdbcTemplate.query("""
                SELECT c.* FROM conversations c
                INNER JOIN conversation_participants cp ON cp.conversation_id = c.id
                WHERE cp.user_id = ?
                ORDER BY c.id DESC
                """, this::mapConversationWithoutParticipants, userId);
        return enrichParticipants(conversations);
    }

    public Optional<Conversation> findConversationById(Long conversationId) {
        List<Conversation> conversations = jdbcTemplate.query("SELECT * FROM conversations WHERE id = ?",
                this::mapConversationWithoutParticipants, conversationId);
        if (conversations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(enrichParticipants(conversations).getFirst());
    }

    public Conversation createConversation(ConversationScene scene, Long orderId, String title) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO conversations (scene, order_id, title)
                    VALUES (?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, scene.name());
            ps.setObject(2, orderId);
            ps.setString(3, title);
            return ps;
        }, keyHolder);
        return findConversationById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public void addParticipants(Long conversationId, Map<Long, Role> roles) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO conversation_participants (conversation_id, user_id, user_role)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE user_role = VALUES(user_role)
                """, roles.entrySet().stream()
                .map(entry -> new Object[] {conversationId, entry.getKey(), entry.getValue().name()})
                .toList());
    }

    public List<Message> findMessages(Long conversationId) {
        return jdbcTemplate.query("""
                SELECT * FROM messages
                WHERE conversation_id = ?
                ORDER BY created_at, id
                """, this::mapMessage, conversationId);
    }

    public Message createMessage(Long conversationId, Long senderId, Role senderRole, MessageType type, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO messages (conversation_id, sender_id, sender_role, message_type, content)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, conversationId);
            ps.setLong(2, senderId);
            ps.setString(3, senderRole.name());
            ps.setString(4, type.name());
            ps.setString(5, content);
            return ps;
        }, keyHolder);
        Long messageId = keyHolder.getKey().longValue();
        return jdbcTemplate.query("SELECT * FROM messages WHERE id = ?", this::mapMessage, messageId).getFirst();
    }

    public List<Conversation> findSupportConversations() {
        List<Conversation> conversations = jdbcTemplate.query("""
                SELECT DISTINCT c.* FROM conversations c
                INNER JOIN conversation_participants cp ON cp.conversation_id = c.id
                WHERE cp.user_role = 'SUPPORT'
                ORDER BY c.id DESC
                """, this::mapConversationWithoutParticipants);
        return enrichParticipants(conversations);
    }

    private List<Conversation> enrichParticipants(List<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return conversations;
        }
        Map<Long, List<Map.Entry<Long, Role>>> groupedParticipants = findParticipants(
                conversations.stream().map(Conversation::id).toList());
        return conversations.stream().map(conversation -> {
            List<Map.Entry<Long, Role>> participants = groupedParticipants.getOrDefault(conversation.id(), List.of());
            Set<Long> participantIds = participants.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
            Map<Long, Role> participantRoles = participants.stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new Conversation(
                    conversation.id(),
                    conversation.scene(),
                    conversation.orderId(),
                    conversation.title(),
                    participantIds,
                    participantRoles);
        }).toList();
    }

    private Map<Long, List<Map.Entry<Long, Role>>> findParticipants(Collection<Long> conversationIds) {
        String placeholders = conversationIds.stream().map(unused -> "?").collect(Collectors.joining(","));
        List<Object> params = conversationIds.stream().map(Object.class::cast).toList();
        return jdbcTemplate.query("""
                SELECT conversation_id, user_id, user_role
                FROM conversation_participants
                WHERE conversation_id IN (""" + placeholders + ")",
                (rs, rowNum) -> Map.entry(
                        rs.getLong("conversation_id"),
                        Map.entry(rs.getLong("user_id"), Role.valueOf(rs.getString("user_role")))),
                params.toArray())
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private Conversation mapConversationWithoutParticipants(ResultSet rs, int rowNum) throws SQLException {
        return new Conversation(
                rs.getLong("id"),
                ConversationScene.valueOf(rs.getString("scene")),
                rs.getObject("order_id", Long.class),
                rs.getString("title"),
                Set.of(),
                Map.of());
    }

    private Message mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new Message(
                rs.getLong("id"),
                rs.getLong("conversation_id"),
                rs.getLong("sender_id"),
                Role.valueOf(rs.getString("sender_role")),
                MessageType.valueOf(rs.getString("message_type")),
                rs.getString("content"),
                rs.getTimestamp("created_at").toInstant());
    }
}
