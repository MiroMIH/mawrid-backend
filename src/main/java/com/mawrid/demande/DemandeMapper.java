package com.mawrid.demande;

import com.mawrid.demande.dto.DemandeAttributeDto;
import com.mawrid.demande.dto.DemandeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DemandeMapper {

    @Mapping(target = "categoryId",       source = "category.id")
    @Mapping(target = "categoryName",     source = "category.name")
    @Mapping(target = "buyerId",          source = "buyer.id")
    @Mapping(target = "buyerCompanyName", source = "buyer.companyName")
    @Mapping(target = "attributes",       source = "attributes")
    DemandeResponse toResponse(Demande demande);

    @Mapping(target = "key",    source = "key")
    @Mapping(target = "value",  source = "value")
    @Mapping(target = "custom", source = "custom")
    DemandeAttributeDto toAttributeDto(DemandeAttribute attribute);
}
