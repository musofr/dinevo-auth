-- Test users with Italian names
-- All passwords are hashed with BCrypt for 'Password123!'

INSERT INTO users (id, email, phoneNumber, password, displayName, userStatus, userType) VALUES
(1, 'giuseppe.rossi@example.it', '+393401234567', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Giuseppe Rossi', 'CONFIRMED', 'CUSTOMER'),
(2, 'maria.bianchi@example.it', '+393472345678', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Maria Bianchi', 'CONFIRMED', 'CUSTOMER'),
(3, 'francesco.ferrari@example.it', '+393383456789', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Francesco Ferrari', 'CONFIRMED', 'CUSTOMER'),
(4, 'lucia.romano@example.it', '+393494567890', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Lucia Romano', 'CONFIRMED', 'CUSTOMER'),
(5, 'marco.esposito@example.it', '+393335678901', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Marco Esposito', 'CONFIRMED', 'CUSTOMER'),
(6, 'anna.colombo@example.it', '+393486789012', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Anna Colombo', 'CONFIRMED', 'CUSTOMER'),
(7, 'paolo.ricci@example.it', '+393457890123', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Paolo Ricci', 'AWAITING_CONFIRMATION', 'CUSTOMER'),
(8, 'giulia.marino@example.it', '+393428901234', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Giulia Marino', 'CONFIRMED', 'CUSTOMER'),
(9, 'andrea.greco@example.it', '+393399012345', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Andrea Greco', 'CONFIRMED', 'MERCHANT'),
(10, 'sofia.conti@example.it', '+393460123456', '$2a$10$jVOCu81ZBCB4vWu1B5vTVuieRdUT0zBswSBvw5NsK8F05vSgIx0E6', 'Sofia Conti', 'CONFIRMED', 'MERCHANT');

INSERT INTO user_establishments (id, user_id, establishment_id, role) VALUES
(1, 9, 1, 'OWNER'), (2, 10, 2, 'OWNER');

-- Reset sequence for PostgreSQL
ALTER SEQUENCE users_seq RESTART WITH 11;
