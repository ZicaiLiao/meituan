package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.SearchRebuildResponse;
import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.User;
import com.meituan.demo.backend.repository.CatalogRepository;
import com.meituan.demo.backend.repository.ChatRepository;
import com.meituan.demo.backend.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final CatalogRepository catalogRepository;

    public AdminService(UserRepository userRepository, ChatRepository chatRepository, CatalogRepository catalogRepository) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.catalogRepository = catalogRepository;
    }

    public List<User> merchants() {
        return usersByRole(Role.MERCHANT);
    }

    public List<User> riders() {
        return usersByRole(Role.RIDER);
    }

    public List<Conversation> supportConversations() {
        return chatRepository.findSupportConversations();
    }

    public SearchRebuildResponse rebuildSearch() {
        return new SearchRebuildResponse(true, "meituan-demo-v1", catalogRepository.countShops(), catalogRepository.countProducts());
    }

    public Map<String, Object> merchantsAndShops() {
        return Map.of("merchants", merchants(), "shops", catalogRepository.findAllShops());
    }

    private List<User> usersByRole(Role role) {
        return userRepository.findByRole(role);
    }
}
