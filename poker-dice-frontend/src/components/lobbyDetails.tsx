import { useEffect, useReducer } from "react";
import { useParams, useNavigate } from "react-router";
import { api, ApiError } from "../api";
import { Lobby, User } from "../types";
import { useAuth } from "../AuthContext";
import "../styles/App.css";

// State type
type State = {
  lobby: Lobby | null;
  host: User | null;
  isLoading: boolean;
  error: string | null;
  isJoining: boolean;
  isLeaving: boolean;
};

// Action types
type Action =
  | { type: "load" }
  | { type: "set-lobby"; lobby: Lobby; host: User }
  | { type: "error"; error: string }
  | { type: "joining" }
  | { type: "leaving" }
  | { type: "action-complete" }
  | { type: "action-error"; error: string };

// Reducer function
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "load":
      return { ...state, isLoading: true, error: null };
    case "set-lobby":
      return {
        ...state,
        lobby: action.lobby,
        host: action.host,
        isLoading: false,
        error: null,
      };
    case "error":
      return { ...state, isLoading: false, error: action.error };
    case "joining":
      return { ...state, isJoining: true };
    case "leaving":
      return { ...state, isLeaving: true };
    case "action-complete":
      return { ...state, isJoining: false, isLeaving: false };
    case "action-error":
      return { ...state, isJoining: false, isLeaving: false, error: action.error };
    default:
      return state;
  }
}

// Initial state
const initialState: State = {
  lobby: null,
  host: null,
  isLoading: true,
  error: null,
  isJoining: false,
  isLeaving: false,
};

export function LobbyDetails() {
  const { lobbyId } = useParams<{ lobbyId: string }>();
  const [state, dispatch] = useReducer(reducer, initialState);
  const { user } = useAuth();
  const navigate = useNavigate();

  // Load lobby data
  useEffect(() => {
    if (!lobbyId) return;

    async function loadLobby() {
      dispatch({ type: "load" });
      try {
        const [lobby, host] = await Promise.all([
          api.getLobbyById(Number(lobbyId)),
          api.getLobbyHost(Number(lobbyId)),
        ]);
        dispatch({ type: "set-lobby", lobby, host });
      } catch (err) {
        if (err instanceof ApiError) {
          dispatch({ type: "error", error: err.message });
        } else {
          dispatch({ type: "error", error: "Erro ao carregar o lobby" });
        }
      }
    }

    loadLobby();
  }, [lobbyId]);

  const handleJoin = async () => {
    if (!lobbyId) return;

    dispatch({ type: "joining" });
    try {
      const result = await api.joinLobby(Number(lobbyId));
      dispatch({ type: "action-complete" });

      // If a match was created, navigate to it
      if (result.matchId) {
        navigate(`/matches/${result.matchId}`);
      } else {
        // Reload lobby to see updated player count
        const [lobby, host] = await Promise.all([
          api.getLobbyById(Number(lobbyId)),
          api.getLobbyHost(Number(lobbyId)),
        ]);
        dispatch({ type: "set-lobby", lobby, host });
      }
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "action-error", error: err.message });
      } else {
        dispatch({ type: "action-error", error: "Erro ao entrar no lobby" });
      }
    }
  };

  const handleLeave = async () => {
    if (!lobbyId) return;

    dispatch({ type: "leaving" });
    try {
      await api.leaveLobby(Number(lobbyId));
      dispatch({ type: "action-complete" });
      // Reload lobby to see updated player count
      const [lobby, host] = await Promise.all([
        api.getLobbyById(Number(lobbyId)),
        api.getLobbyHost(Number(lobbyId)),
      ]);
      dispatch({ type: "set-lobby", lobby, host });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "action-error", error: err.message });
      } else {
        dispatch({ type: "action-error", error: "Erro ao sair do lobby" });
      }
    }
  };

  if (state.isLoading) {
    return <div className="lobby-details-loading">A carregar lobby...</div>;
  }

  if (state.error || !state.lobby) {
    return (
      <div className="lobby-details-error">
        {state.error || "Lobby não encontrado"}
      </div>
    );
  }

  const { lobby, host } = state;
  const players = lobby.players ?? [];
  const isInLobby = user && players.includes(user.id);
  const canJoin = user && lobby.state === "OPEN" && !isInLobby && players.length < lobby.maxPlayers;
  const canLeave = user && isInLobby && lobby.state === "OPEN";

  return (
    <div className="lobby-details-container">
      <div className="lobby-details-header">
        <h2>{lobby.name}</h2>
        <p>{lobby.description}</p>
        <div className="lobby-details-meta">
          <span>Host: {host?.name || "Desconhecido"}</span>
          <span>
            Jogadores: {players.length}/{lobby.maxPlayers}
          </span>
          <span>Mínimo: {lobby.minPlayers} jogadores</span>
          <span>Rondas: {lobby.rounds}</span>
          <span>Ante: {lobby.ante}€</span>
          <span className={`lobby-state lobby-state-${lobby.state.toLowerCase()}`}>
            Estado:{" "}
            {lobby.state === "OPEN"
              ? "Aberto"
              : lobby.state === "FULL"
              ? "Fechado"
              : "Em Jogo"}
          </span>
        </div>
      </div>

      <div className="lobby-details-actions">
        {state.error && (
          <div className="lobby-details-error-message">{state.error}</div>
        )}

        {canJoin && (
          <button
            onClick={handleJoin}
            disabled={state.isJoining}
            className="lobby-join-btn"
          >
            {state.isJoining ? "A entrar..." : "Entrar no Lobby"}
          </button>
        )}

        {canLeave && (
          <button
            onClick={handleLeave}
            disabled={state.isLeaving}
            className="lobby-leave-btn"
          >
            {state.isLeaving ? "A sair..." : "Sair do Lobby"}
          </button>
        )}

        {isInLobby && lobby.state !== "OPEN" && (
          <p className="lobby-in-game-message">
            {lobby.state === "STARTED"
              ? "O jogo está em andamento!"
              : "Este lobby está fechado."}
          </p>
        )}

        {!user && (
          <p className="lobby-login-message">
            Faça login para entrar neste lobby.
          </p>
        )}
      </div>

      <div className="lobby-details-players">
        <h3>Jogadores ({players.length})</h3>
        {players.length === 0 ? (
          <p>Nenhum jogador ainda.</p>
        ) : (
          <ul className="lobby-players-list">
            {players.map((playerId) => (
              <li key={playerId} className="lobby-player-item">
                Jogador #{playerId}
                {playerId === lobby.hostUserId && (
                  <span className="lobby-host-badge">Host</span>
                )}
                {user && playerId === user.id && (
                  <span className="lobby-you-badge">Você</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}