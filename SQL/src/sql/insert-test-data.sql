-- Inserir um utilizador na tabela dbo.users
INSERT INTO dbo.users (name, email, password_validation)
VALUES ('Utilizador Demo', 'demo@example.com', 'hash_demo');

-- Inserir um utilizador na tabela app_user
INSERT INTO app_user (username, display_name, password_hash, balance_coins)
VALUES ('demo_user', 'Demo User', 'hash_demo', 100)
RETURNING id;

-- Inserir uma ronda (usa defaults nos ENUMs)
INSERT INTO round (pot, results)
VALUES (10, 'sem resultados ainda');
