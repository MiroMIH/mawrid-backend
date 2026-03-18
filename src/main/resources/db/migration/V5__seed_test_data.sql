-- V5: Test seed data for development
-- Note: Flyway is disabled. Run manually via psql if needed.
-- Passwords are bcrypt of "password123"

-- Test buyer users
INSERT INTO users (id, email, password, first_name, last_name, phone, company_name, wilaya, role, enabled)
VALUES
  ('a1b2c3d4-0001-0001-0001-000000000001', 'buyer1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Karim', 'Benali', '0550001001', 'SONATRACH Approvisionnements', 'Alger', 'BUYER', true),
  ('a1b2c3d4-0002-0002-0002-000000000002', 'buyer2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Amira', 'Cherif', '0550002002', 'COSIDER Construction', 'Annaba', 'BUYER', true),
  ('a1b2c3d4-0003-0003-0003-000000000003', 'buyer3@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Mourad', 'Hamdi', '0550003003', 'ENIEM Tizi-Ouzou', 'Tizi Ouzou', 'BUYER', true)
ON CONFLICT (email) DO NOTHING;

-- Test supplier users
INSERT INTO users (id, email, password, first_name, last_name, phone, company_name, wilaya, role, enabled)
VALUES
  ('b1b2c3d4-0001-0001-0001-000000000001', 'supplier1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Rachid', 'Mekki', '0660001001', 'El Hidhab Roulements', 'Annaba', 'SUPPLIER', true),
  ('b1b2c3d4-0002-0002-0002-000000000002', 'supplier2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Fatima', 'Zouai', '0660002002', 'Mecatech Fournitures', 'Alger', 'SUPPLIER', true),
  ('b1b2c3d4-0003-0003-0003-000000000003', 'supplier3@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Hassan', 'Boukhari', '0660003003', 'TransAlgerie Logistique', 'Oran', 'SUPPLIER', true),
  ('b1b2c3d4-0004-0004-0004-000000000004', 'supplier4@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Nadir', 'Khelif', '0660004004', 'Equip Pro Sétif', 'Sétif', 'SUPPLIER', true),
  ('b1b2c3d4-0005-0005-0005-000000000005', 'supplier5@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'Samia', 'Bensaid', '0660005005', 'Constantine Equipements', 'Constantine', 'SUPPLIER', true)
ON CONFLICT (email) DO NOTHING;
