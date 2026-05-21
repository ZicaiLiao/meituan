package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.RiderAvailabilityRequest;
import com.meituan.demo.backend.repository.UserRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOpsService {

    private final UserRepository userRepository;

    public UserOpsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> riderProfile(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Rider not found"));
        var profile = userRepository.findRiderProfile(userId)
                .orElseThrow(() -> new IllegalArgumentException("Rider profile not found"));
        return Map.of(
                "user", user,
                "online", profile.online(),
                "capacity", profile.capacity(),
                "vehicleType", profile.vehicleType());
    }

    @Transactional
    public void updateRiderAvailability(Long userId, RiderAvailabilityRequest request) {
        userRepository.updateRiderAvailability(userId, request.online(), request.capacity());
    }
}
