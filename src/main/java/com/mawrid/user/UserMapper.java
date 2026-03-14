package com.mawrid.user;

import com.mawrid.category.Category;
import com.mawrid.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "categoryIds", source = "categories", qualifiedByName = "categoriesToIds")
    UserResponse toResponse(User user);

    @Named("categoriesToIds")
    default List<Long> categoriesToIds(Set<Category> categories) {
        if (categories == null) return List.of();
        return categories.stream().map(Category::getId).collect(Collectors.toList());
    }
}
