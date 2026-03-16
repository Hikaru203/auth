-- Fix incorrect admin password hash
UPDATE users 
SET password_hash = '$2a$12$j4DD/qS.AyHWHHxiZmseS.LQoAfr3uns7vz2eo47c7ApuS3Vj5WiW' 
WHERE username = 'admin' AND tenant_id = '00000000-0000-0000-0000-000000000001';
