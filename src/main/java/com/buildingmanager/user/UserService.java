package com.buildingmanager.user;

import com.buildingmanager.role.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean updateUserRole(Integer userId, Role newRole) {
        return userRepository.findById(userId).map(user -> {
            user.getRoles().clear();
            user.getRoles().add(newRole);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}