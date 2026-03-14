-- ============================================================
-- V2 – Seed Algerian Industrial Categories
-- ============================================================

-- Top-level categories
INSERT INTO categories (name, slug, parent_id) VALUES
    ('Machines Agricoles',          'machines-agricoles',         NULL),
    ('Pièces Mécaniques',           'pieces-mecaniques',          NULL),
    ('Matériaux de Construction',   'materiaux-construction',     NULL),
    ('Équipements Électriques',     'equipements-electriques',    NULL),
    ('Équipements Industriels',     'equipements-industriels',    NULL),
    ('Produits Chimiques',          'produits-chimiques',         NULL),
    ('Emballage & Conditionnement', 'emballage-conditionnement',  NULL),
    ('Fournitures de Bureau',       'fournitures-bureau',         NULL),
    ('Hydraulique & Pneumatique',   'hydraulique-pneumatique',    NULL),
    ('Sécurité Industrielle',       'securite-industrielle',      NULL);

-- Sub-categories: Machines Agricoles (id=1)
INSERT INTO categories (name, slug, parent_id) VALUES
    ('Tracteurs',             'tracteurs',              1),
    ('Moissonneuses-Batteuses', 'moissonneuses-batteuses', 1),
    ('Motopompes',            'motopompes',             1),
    ('Systèmes d''Irrigation', 'systemes-irrigation',    1);

-- Sub-categories: Pièces Mécaniques (id=2)
INSERT INTO categories (name, slug, parent_id) VALUES
    ('Roulements',            'roulements',             2),
    ('Courroies & Chaînes',   'courroies-chaines',      2),
    ('Joints & Garnitures',   'joints-garnitures',      2),
    ('Engrenages',            'engrenages',             2);

-- Sub-categories: Matériaux de Construction (id=3)
INSERT INTO categories (name, slug, parent_id) VALUES
    ('Ciment & Béton',        'ciment-beton',           3),
    ('Acier & Ferraille',     'acier-ferraille',        3),
    ('Brique & Tuile',        'brique-tuile',           3),
    ('Isolation Thermique',   'isolation-thermique',    3);

-- Sub-categories: Équipements Électriques (id=4)
INSERT INTO categories (name, slug, parent_id) VALUES
    ('Câbles & Fils',         'cables-fils',            4),
    ('Tableaux Électriques',  'tableaux-electriques',   4),
    ('Moteurs Électriques',   'moteurs-electriques',    4),
    ('Groupes Électrogènes',  'groupes-electrogenes',   4);
