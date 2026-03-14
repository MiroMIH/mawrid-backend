package com.mawrid.category;

import com.mawrid.category.dto.CategoryAttributeResponse;
import com.mawrid.category.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", source = "children")
    CategoryResponse toResponse(Category category);

    @Mapping(target = "id",           source = "id")
    @Mapping(target = "key",          source = "key")
    @Mapping(target = "label",        source = "label")
    @Mapping(target = "type",         source = "type")
    @Mapping(target = "required",     source = "required")
    @Mapping(target = "inherited",    source = "inherited")
    @Mapping(target = "displayOrder", source = "displayOrder")
    CategoryAttributeResponse toAttributeResponse(CategoryAttribute attribute);
}
