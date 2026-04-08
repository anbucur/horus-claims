-- V2__Seed_Test_Data.sql
-- Seed test data for MSIG Claims Platform

-- Insert Policies
INSERT INTO policy (policy_number, effective_date, expiration_date, line_of_business, coverage_limits, deductibles, status, created_at, updated_at)
VALUES 
    ('MSC-HULL-2024-8891', '2024-01-01', '2024-12-31', 'Hull & Machinery', 15000000.00, 50000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('EURCARGO-2024-4455', '2024-01-01', '2024-12-31', 'Cargo', 5000000.00, 25000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('NORDIC-TANKER-2024-7721', '2024-01-01', '2024-12-31', 'Hull & Machinery', 25000000.00, 100000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Parties
INSERT INTO party (party_type, name, email, phone, address, country, created_at)
VALUES 
    ('ASSURED', 'OceanTech Shipping Ltd', 'operations@oceantech.com', '+31-20-1234567', 'Amsterdam Port, Netherlands', 'Netherlands', CURRENT_TIMESTAMP),
    ('BROKER', 'Marsh Ltd Rotterdam', 'j.visser@marsh.com', '+31-10-9876543', 'Rotterdam, Netherlands', 'Netherlands', CURRENT_TIMESTAMP),
    ('BROKER', 'Maersk Chartering B.V.', 'claims@maersk.com', '+45-33-123456', 'Copenhagen, Denmark', 'Denmark', CURRENT_TIMESTAMP),
    ('ASSURED', 'Nordic Tanker AS', 'ops@nordictanker.no', '+47-22-987654', 'Oslo, Norway', 'Norway', CURRENT_TIMESTAMP);

-- Insert Subject Matter Insured (Vessels)
INSERT INTO subject_matter_insured (claim_id, type, name, imo_number, vessel_type, cargo_description, tonnage, created_at)
VALUES 
    (1, 'VESSEL', 'MSC Oscar', 'IMO 9703291', 'Container Vessel', 'Container cargo', 4500, CURRENT_TIMESTAMP),
    (2, 'VESSEL', 'MSC Attikos', 'IMO 9612345', 'Container Vessel', 'Container cargo', 4200, CURRENT_TIMESTAMP),
    (3, 'VESSEL', 'Euro Carrier III', 'IMO 9187654', 'General Cargo', 'General cargo', 2800, CURRENT_TIMESTAMP);

-- Insert Claims
INSERT INTO claim (policy_id, date_of_loss, incident_narrative, loss_location, ai_confidence_score, workflow_status, created_at, updated_at)
VALUES 
    (1, '2024-11-12', 'Hull breach to starboard side due to pier allision during berthing maneuvers. Damage to plating and rudder. Vessel MSC Oscar was docking at Rotterdam Berth 7 when strong wind gust caused the vessel to impact the concrete pier structure. Significant hull deformation reported.', 'Rotterdam Berth 7, Netherlands', 0.87, 'EXTRACTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2024-10-28', 'Cargo damage to 12 containers due to heavy storm encountered in North Atlantic. Containers secured on deck shifted and sustained water ingress damage. Insurance survey conducted via video call with vessel master.', 'North Atlantic - position 54N 25W', 0.93, 'STP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, '2024-11-15', 'Container collapse in cargo hold during loading operations at Antwerp port. Forklift incident caused stack of 8 containers to topple, resulting in total loss of 4 containers and damage to 6 others. No personnel injuries reported.', 'Antwerp Port, Belgium', 0.0, 'RECEIVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Claim-Party relationships
INSERT INTO claim_party (claim_id, party_id, party_role)
VALUES 
    (1, 1, 'ASSURED'),
    (1, 2, 'BROKER'),
    (2, 1, 'ASSURED'),
    (2, 3, 'BROKER'),
    (3, 4, 'ASSURED');

-- Insert Evidence
INSERT INTO evidence (claim_id, document_type, file_name, file_url, ai_classification_tags, forensics_score, is_quarantined, created_at)
VALUES 
    (1, 'IMAGE', 'hull_damage_1.jpg', '/storage/claims/1/evidence/hull_damage_1.jpg', 'hull_damage,berth_incident,structural', 0.95, false, CURRENT_TIMESTAMP),
    (1, 'IMAGE', 'hull_damage_2.jpg', '/storage/claims/1/evidence/hull_damage_2.jpg', 'hull_damage,berth_incident', 0.92, false, CURRENT_TIMESTAMP),
    (1, 'SURVEY_REPORT', 'docking_survey.pdf', '/storage/claims/1/evidence/docking_survey.pdf', 'survey_report,docking', 0.98, false, CURRENT_TIMESTAMP),
    (2, 'IMAGE', 'container_damage_1.jpg', '/storage/claims/1/evidence/container_damage_1.jpg', 'cargo_damage,storm,water_ingress', 0.89, false, CURRENT_TIMESTAMP),
    (2, 'EMAIL', 'master_report.eml', '/storage/claims/2/evidence/master_report.eml', 'email,incident_report', null, false, CURRENT_TIMESTAMP),
    (3, 'IMAGE', 'container_collapse_1.jpg', '/storage/claims/3/evidence/container_collapse_1.jpg', 'cargo_incident,forklift,collapse', null, false, CURRENT_TIMESTAMP),
    (3, 'SURVEY_REPORT', 'antwerp_survey.pdf', '/storage/claims/3/evidence/antwerp_survey.pdf', 'survey_report,port_incident', null, false, CURRENT_TIMESTAMP);

-- Insert Financials (reserves and payments)
INSERT INTO financials (claim_id, category, amount, currency, status, transaction_date, created_at)
VALUES 
    (1, 'INDEMNITY', 2400000.00, 'EUR', 'RESERVE', '2024-11-15', CURRENT_TIMESTAMP),
    (2, 'INDEMNITY', 850000.00, 'EUR', 'PAYMENT', '2024-11-10', CURRENT_TIMESTAMP),
    (3, 'INDEMNITY', 320000.00, 'EUR', 'RESERVE', '2024-11-16', CURRENT_TIMESTAMP);
