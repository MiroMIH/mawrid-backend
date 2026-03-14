package com.mawrid.user;

import com.mawrid.category.Category;
import com.mawrid.category.CategoryRepository;
import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.ResourceNotFoundException;
import com.mawrid.user.dto.FcmTokenRequest;
import com.mawrid.user.dto.UpdateCategoriesRequest;
import com.mawrid.user.dto.UpdateUserRequest;
import com.mawrid.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findOrThrow(id);

        if (request.getFirstName()      != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()       != null) user.setLastName(request.getLastName());
        if (request.getPhone()          != null) user.setPhone(request.getPhone());
        if (request.getCompanyName()    != null) user.setCompanyName(request.getCompanyName());
        if (request.getWilaya()         != null) user.setWilaya(request.getWilaya());
        if (request.getRegistreCommerce() != null) user.setRegistreCommerce(request.getRegistreCommerce());

        if (request.getCategoryIds() != null && user.getRole() == Role.SUPPLIER) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            user.setCategories(new HashSet<>(categories));
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateCategories(UUID id, UpdateCategoriesRequest request, User currentUser) {
        if (currentUser.getRole() != Role.SUPPLIER) {
            throw new BusinessException("Only suppliers can manage category subscriptions", HttpStatus.FORBIDDEN);
        }

        User user = findOrThrow(id);
        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
        user.setCategories(new HashSet<>(categories));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void updateFcmToken(UUID id, FcmTokenRequest request) {
        User user = findOrThrow(id);
        user.setFcmToken(request.getFcmToken());
        userRepository.save(user);
    }

    public User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
