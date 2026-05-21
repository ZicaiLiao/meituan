package com.meituan.demo.backend.persistence.mapper;

import com.meituan.demo.backend.persistence.PersistenceEntities.ConversationEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.ConversationParticipantEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.MessageEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ChatMapper {

    @Select("""
            SELECT c.id, c.scene, c.order_id, c.title
            FROM conversations c
            INNER JOIN conversation_participants cp ON cp.conversation_id = c.id
            WHERE cp.user_id = #{userId}
            ORDER BY c.id DESC
            """)
    List<ConversationEntity> findConversationsByUserId(@Param("userId") Long userId);

    @Select("SELECT id, scene, order_id, title FROM conversations WHERE id = #{conversationId}")
    ConversationEntity findConversationById(@Param("conversationId") Long conversationId);

    @Insert("""
            INSERT INTO conversations (scene, order_id, title)
            VALUES (#{scene}, #{orderId}, #{title})
            """)
    int insertConversation(@Param("scene") String scene, @Param("orderId") Long orderId, @Param("title") String title);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Insert("""
            INSERT INTO conversation_participants (conversation_id, user_id, user_role)
            VALUES (#{conversationId}, #{userId}, #{userRole})
            ON DUPLICATE KEY UPDATE user_role = VALUES(user_role)
            """)
    int insertParticipant(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("userRole") String userRole);

    @Select("""
            <script>
            SELECT conversation_id, user_id, user_role
            FROM conversation_participants
            WHERE conversation_id IN
            <foreach collection='conversationIds' item='conversationId' open='(' separator=',' close=')'>
                #{conversationId}
            </foreach>
            </script>
            """)
    List<ConversationParticipantEntity> findParticipants(@Param("conversationIds") Collection<Long> conversationIds);

    @Select("""
            SELECT id, conversation_id, sender_id, sender_role, message_type, content, created_at
            FROM messages
            WHERE conversation_id = #{conversationId}
            ORDER BY created_at, id
            """)
    List<MessageEntity> findMessages(@Param("conversationId") Long conversationId);

    @Insert("""
            INSERT INTO messages (conversation_id, sender_id, sender_role, message_type, content, created_at)
            VALUES (#{conversationId}, #{senderId}, #{senderRole}, #{messageType}, #{content}, #{createdAt})
            """)
    int insertMessage(
            @Param("conversationId") Long conversationId,
            @Param("senderId") Long senderId,
            @Param("senderRole") String senderRole,
            @Param("messageType") String messageType,
            @Param("content") String content,
            @Param("createdAt") Instant createdAt);

    @Select("""
            SELECT id, conversation_id, sender_id, sender_role, message_type, content, created_at
            FROM messages
            WHERE id = #{messageId}
            """)
    MessageEntity findMessageById(@Param("messageId") Long messageId);

    @Select("""
            SELECT DISTINCT c.id, c.scene, c.order_id, c.title
            FROM conversations c
            INNER JOIN conversation_participants cp ON cp.conversation_id = c.id
            WHERE cp.user_role = 'SUPPORT'
            ORDER BY c.id DESC
            """)
    List<ConversationEntity> findSupportConversations();
}
