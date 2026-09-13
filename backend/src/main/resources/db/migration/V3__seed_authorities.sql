-- V3: Seed Deterministic Assessment / Demo Authority and Jurisdiction Records
-- NOTICE: These are fictional demo datasets for evaluation and automated testing purposes only.
-- They do NOT represent official government administrative borders or actual survey data.

-- 1. Demo Municipal Authority: New Delhi Municipal Council (NDMC Demo)
INSERT INTO civic_authorities (id, name, code, contact_email, contact_phone, department_type)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'DEMO New Delhi Municipal Council (NDMC)',
    'DEMO_NDMC_CENTRAL',
    'reports-demo@ndmc.gov.in.test',
    '+91-11-23340001',
    'MUNICIPAL'
);

-- NDMC Municipal Polygon Boundary (Covers Longitude 77.18 to 77.26, Latitude 28.58 to 28.66)
INSERT INTO authority_jurisdictions (id, authority_id, jurisdiction_type, geometry)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'MUNICIPAL_BOUNDARY',
    ST_SetSRID(ST_GeomFromText('POLYGON((77.1800 28.5800, 77.2600 28.5800, 77.2600 28.6600, 77.1800 28.6600, 77.1800 28.5800))'), 4326)
);

-- 2. Demo Municipal Authority: Municipal Corporation of Delhi - North Zone (MCD North Demo)
INSERT INTO civic_authorities (id, name, code, contact_email, contact_phone, department_type)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'DEMO Municipal Corporation of Delhi (North Zone)',
    'DEMO_MCD_NORTH',
    'reports-demo@mcd.gov.in.test',
    '+91-11-23340002',
    'MUNICIPAL'
);

-- MCD North Municipal Polygon Boundary (Covers Longitude 77.10 to 77.26, Latitude 28.66 to 28.75)
INSERT INTO authority_jurisdictions (id, authority_id, jurisdiction_type, geometry)
VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    'MUNICIPAL_BOUNDARY',
    ST_SetSRID(ST_GeomFromText('POLYGON((77.1000 28.6600, 77.2600 28.6600, 77.2600 28.7500, 77.1000 28.7500, 77.1000 28.6600))'), 4326)
);

-- 3. Demo Dedicated Road Authority: Public Works Department - State Arterial Division (PWD Demo)
INSERT INTO civic_authorities (id, name, code, contact_email, contact_phone, department_type)
VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'DEMO Delhi Public Works Department (Arterial Roads Division)',
    'DEMO_PWD_ARTERIAL',
    'arterial-roads-demo@pwd.delhi.gov.in.test',
    '+91-11-23340003',
    'PWD'
);

-- PWD Dedicated Road Network Line (Inner Ring Road Demo Segment intersecting municipal zones)
INSERT INTO authority_jurisdictions (id, authority_id, jurisdiction_type, geometry)
VALUES (
    'b0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000003',
    'ROAD_NETWORK',
    ST_SetSRID(ST_GeomFromText('MULTILINESTRING((77.2000 28.6000, 77.2200 28.6200, 77.2400 28.6400))'), 4326)
);

-- 4. Demo Dedicated Road Authority: National Highways Authority of India (NHAI Demo)
INSERT INTO civic_authorities (id, name, code, contact_email, contact_phone, department_type)
VALUES (
    'a0000000-0000-0000-0000-000000000004',
    'DEMO National Highways Authority of India (NHAI)',
    'DEMO_NHAI_HIGHWAY',
    'highways-demo@nhai.gov.in.test',
    '+91-11-25074100',
    'HIGHWAY'
);

-- NHAI Dedicated Highway Road Network Line (NH-44 Corridor Demo Segment)
INSERT INTO authority_jurisdictions (id, authority_id, jurisdiction_type, geometry)
VALUES (
    'b0000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000004',
    'ROAD_NETWORK',
    ST_SetSRID(ST_GeomFromText('MULTILINESTRING((77.1200 28.7000, 77.1300 28.7200, 77.1400 28.7400))'), 4326)
);
