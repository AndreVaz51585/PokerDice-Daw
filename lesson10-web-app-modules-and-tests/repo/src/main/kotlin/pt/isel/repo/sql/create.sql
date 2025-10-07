-- Ficheiro com as instruções SQL para criar as tabelas da base de dados para a minha pasta do domain Game
CREATE TABLE IF NOT EXISTS Game (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    release_date DATE NOT NULL,
    developer VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS Combination (
    id SERIAL PRIMARY KEY,
    game_id INT NOT NULL,
    combination VARCHAR(255) NOT NULL,
    FOREIGN KEY (game_id) REFERENCES Game(id) ON DELETE CASCADE
);

