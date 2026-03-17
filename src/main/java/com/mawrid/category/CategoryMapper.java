package com.mawrid.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mawrid.category.dto.CategoryAttributeResponse;
import com.mawrid.category.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /** Flat response for a single node (no recursive children). */
    @Mapping(target = "parentId",      source = "parent.id")
    @Mapping(target = "parentName",    source = "parent.name")
    @Mapping(target = "childrenCount", expression = "java(category.getChildren().size())")
    CategoryResponse toResponse(Category category);

    /**
     * Attribute response — {@code inherited} is always set to false here;
     * callers must override it when the attribute comes from an ancestor.
     */
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "inherited",  constant = "false")
    @Mapping(target = "options",    expression = "java(parseOptions(attribute.getOptions()))")
    CategoryAttributeResponse toAttributeResponse(CategoryAttribute attribute);

    ObjectMapper _MAPPER = new ObjectMapper();

    default List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return _MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
