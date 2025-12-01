import { Link } from "react-router";
import { Lobby } from "../types";
import { useFetch } from "../hooks/useFetch";
import "../styles/App.css";

export function LobbiesList() {
  const state = useFetch<Lobby[]>("/lobbies");

  if (state.type === "begin" || state.type === "loading") {
    return <div className="lobbies-list-loading">A carregar lobbies...</div>;
  }

  if (state.type === "error") {
    return <div className="lobbies-list-error">{state.error.message}</div>;
  }

  const lobbies = state.payload ?? [];

  return (
    <div className="lobbies-list-container">
      <h2>Lobbies Disponíveis</h2>
      {lobbies.length === 0 ? (
        <p className="lobbies-list-empty">
          Nenhum lobby encontrado. Crie um para começar!
        </p>
      ) : (
        <div className="lobbies-list-grid">
          {lobbies.map((lobby) => {
              const players = lobby.players ?? [];
            return (
            <Link
              key={lobby.id}
              to={`/lobbies/${lobby.id}`}
              className="lobbies-list-link"
            >
              <div className="lobbies-list-card">
                <h3>{lobby.name}</h3>
                <p>{lobby.description}</p>
                <div className="lobbies-list-card-meta">
                  <span>
                    Jogadores: {players.length}/{lobby.maxPlayers}
                  </span>
                  <span>Rondas: {lobby.rounds}</span>
                  <span>Ante: {lobby.ante}€</span>
                  <span className={`lobby-state lobby-state-${lobby.state.toLowerCase()}`}>
                    {lobby.state === "OPEN"
                      ? "Aberto"
                      : lobby.state === "FULL"
                      ? "Fechado"
                      : "Em Jogo"}
                  </span>
                </div>
              </div>
            </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}