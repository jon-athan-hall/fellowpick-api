package com.fellowpick.api.user;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        Iterable<User> users = userRepository.findAll();

        return StreamSupport.stream(users.spliterator(), false)
                .collect(Collectors.toList());
    }
}
