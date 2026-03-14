package com.mawrid.common.config;

import com.mawrid.category.Category;
import com.mawrid.category.CategoryRepository;
import com.mawrid.common.enums.NodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the 10 top-level sectors and their subcategories on first boot
 * when the categories table is empty (replaces V2 Flyway migration).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.debug("Categories already seeded — skipping DataInitializer");
            return;
        }

        log.info("Seeding initial category data...");

        Category machines     = root("Machines Agricoles",          "machines-agricoles");
        Category pieces       = root("Pièces Mécaniques",           "pieces-mecaniques");
        Category materiaux    = root("Matériaux de Construction",   "materiaux-construction");
        Category electriques  = root("Équipements Électriques",     "equipements-electriques");
        Category industriels  = root("Équipements Industriels",     "equipements-industriels");
        Category chimiques    = root("Produits Chimiques",          "produits-chimiques");
        Category emballage    = root("Emballage & Conditionnement", "emballage-conditionnement");
        Category fournitures  = root("Fournitures de Bureau",       "fournitures-bureau");
        Category hydraulique  = root("Hydraulique & Pneumatique",   "hydraulique-pneumatique");
        Category securite     = root("Sécurité Industrielle",       "securite-industrielle");

        // Machines Agricoles children
        child("Tracteurs",               "tracteurs",               machines);
        child("Moissonneuses-Batteuses", "moissonneuses-batteuses", machines);
        child("Motopompes",              "motopompes",              machines);
        child("Systèmes d'Irrigation",   "systemes-irrigation",     machines);

        // Pièces Mécaniques children
        child("Roulements",          "roulements",      pieces);
        child("Courroies & Chaînes", "courroies-chaines", pieces);
        child("Joints & Garnitures", "joints-garnitures", pieces);
        child("Engrenages",          "engrenages",      pieces);

        // Matériaux de Construction children
        child("Ciment & Béton",      "ciment-beton",       materiaux);
        child("Acier & Ferraille",   "acier-ferraille",    materiaux);
        child("Brique & Tuile",      "brique-tuile",       materiaux);
        child("Isolation Thermique", "isolation-thermique", materiaux);

        // Équipements Électriques children
        child("Câbles & Fils",        "cables-fils",         electriques);
        child("Tableaux Électriques", "tableaux-electriques", electriques);
        child("Moteurs Électriques",  "moteurs-electriques", electriques);
        child("Groupes Électrogènes", "groupes-electrogenes", electriques);

        // Équipements Industriels children
        child("Compresseurs",    "compresseurs",    industriels);
        child("Pompes",          "pompes",          industriels);
        child("Convoyeurs",      "convoyeurs",      industriels);
        child("Chaudières",      "chaudieres",      industriels);

        // Produits Chimiques children
        child("Lubrifiants",         "lubrifiants",          chimiques);
        child("Solvants",            "solvants",             chimiques);
        child("Produits de Nettoyage", "produits-nettoyage", chimiques);

        // Emballage children
        child("Cartons & Boîtes",   "cartons-boites",    emballage);
        child("Sachets & Films",    "sachets-films",     emballage);
        child("Étiquettes",         "etiquettes",        emballage);

        // Fournitures children
        child("Papeterie",          "papeterie",     fournitures);
        child("Mobilier de Bureau", "mobilier",      fournitures);

        // Hydraulique children
        child("Vérins Hydrauliques", "verins-hydrauliques", hydraulique);
        child("Vannes & Raccords",   "vannes-raccords",     hydraulique);
        child("Compresseurs Air",    "compresseurs-air",    hydraulique);

        // Sécurité children
        child("EPI",                      "epi",                  securite);
        child("Extincteurs",              "extincteurs",          securite);
        child("Signalisation de Sécurité","signalisation-securite", securite);

        log.info("Category seeding complete.");
    }

    private Category root(String name, String slug) {
        Category c = categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .path("placeholder")
                .depth(0)
                .nodeType(NodeType.SEEDED)
                .build());
        c.setPath(String.valueOf(c.getId()));
        return categoryRepository.save(c);
    }

    private Category child(String name, String slug, Category parent) {
        Category c = categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .path("placeholder")
                .depth(parent.getDepth() + 1)
                .nodeType(NodeType.SEEDED)
                .build());
        c.setPath(parent.getPath() + "." + c.getId());
        return categoryRepository.save(c);
    }
}
