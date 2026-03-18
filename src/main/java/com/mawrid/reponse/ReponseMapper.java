package com.mawrid.reponse;

import com.mawrid.reponse.dto.ReponseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReponseMapper {

    @Mapping(target = "demandeId",           source = "demande.id")
    @Mapping(target = "demandeTitle",        source = "demande.title")
    @Mapping(target = "supplierId",          source = "supplier.id")
    @Mapping(target = "supplierEmail",       source = "supplier.email")
    @Mapping(target = "supplierFirstName",   source = "supplier.firstName")
    @Mapping(target = "supplierLastName",    source = "supplier.lastName")
    @Mapping(target = "supplierPhone",       source = "supplier.phone")
    @Mapping(target = "supplierCompanyName", source = "supplier.companyName")
    @Mapping(target = "supplierWilaya",      source = "supplier.wilaya")
    @Mapping(target = "supplierScore",       ignore = true)
    ReponseResponse toResponse(Reponse reponse);
}
