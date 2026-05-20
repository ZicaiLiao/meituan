package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.SearchRebuildResponse;
import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.User;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final DemoDataStore dataStore;

    public AdminService(DemoDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<User> merchants() {
        return usersByRole(Role.MERCHANT);
    }

    public List<User> riders() {
        return usersByRole(Role.RIDER);
    }

    public List<Conversation> supportConversations() {
        return dataStore.conversations().values().stream()
                .filter(conversation -> conversation.participantRoles().containsValue(Role.SUPPORT))
                .toList();
    }

    public SearchRebuildResponse rebuildSearch() {
        return new SearchRebuildResponse(true, "meituan-demo-v1", dataStore.shops().size(), dataStore.products().size());
    }

    public Map<String, Object> merchantsAndShops() {
        return Map.of("merchants", merchants(), "shops", dataStore.shops().values());
    }

    private List<User> usersByRole(Role role) {
        return dataStore.users().values().stream()
                .filter(user -> user.role() == role)
                .toList();
    }
}

