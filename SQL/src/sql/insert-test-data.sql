-- Inserir um utilizador na tabela dbo.users
INSERT INTO dbo.users (name, email, password_validation)
VALUES ('Utilizador Demo', 'demo@example.com', 'hash_demo');


-- Inserir uma ronda (usa defaults nos ENUMs)
INSERT INTO round (pot, results)
VALUES (10, 'sem resultados ainda');

