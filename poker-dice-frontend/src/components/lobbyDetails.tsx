import { useEffect, useReducer, useCallback } from "react";
import { useParams, useNavigate } from "react-router";
import { api, ApiError } from "../api";
import {Lobby, Player, User} from "../types";
import { useAuth } from "../AuthContext";
import {
    useLobbyListener,
    playerJoinedData,
    playerLeftData,
    LobbySSEMessage,
    matchStartingData
} from "../hooks/useLobbyEvents.tsx";
import "../styles/App.css";

// State type
type State = {
    lobby: Lobby | null;
    players: Player[];
    host: User | null;
    matchId: number | null;
    isLoading: boolean;
    isJoining: boolean;
    isLeaving: boolean;
    isStartingMatch: boolean;
    error: string | null;
};

// Action types
type Action =
    | { type: "load" }
    | { type: "set-lobby"; lobby: Lobby; host: User; players: Player[] }
    | { type: "error"; error: string }
    | { type: "player-join"; data: playerJoinedData }
    | { type: "player-left"; data: playerLeftData }
    | { type: "match-starting"; data: matchStartingData }
    | { type: "joining" }
    | { type: "leaving" }
    | { type: "startingMatch" }
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
                players: action.players,
                isLoading: false,
                error: null,
            };
        case "error":
            return { ...state, isLoading: false, error: action.error };
        case "player-join":
            return handlePlayerJoin(state, action);
        case "player-left":
            return handlePlayerLeft(state, action);
        case "joining":
            return { ...state, isJoining: true, error: null };
        case "leaving":
            return { ...state, isLeaving: true, error: null };
        case "match-starting":
            return { ...state, matchId: action.data.matchId, isStartingMatch: true };
        case "action-complete":
            return { ...state, isJoining: false, isLeaving: false, error: null };
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
    players: [],
    matchId: null,
    isLoading: true,
    isJoining: false,
    isLeaving: false,
    isStartingMatch : false,
    error: null,
};

function handlePlayerJoin(state: State, action: { type: "player-join"; data: playerJoinedData }): State {


    // Verifica se o jogador já existe
    const playerExists = state.players.some(p => p.id === action.data.player.id);
    if (playerExists) {
        return state;
    }

    const newPlayers = [...state.players, action.data.player];

    // Atualiza estado do lobby se estiver cheio
    if (newPlayers.length >= action.data.maxPlayers && state.lobby) {
        const updatedLobby: Lobby = { ...state.lobby, state: "FULL" };
        return { ...state, players: newPlayers, lobby: updatedLobby };
    }

    return { ...state, players: newPlayers };
}

function handlePlayerLeft(state: State, action: { type: "player-left"; data: playerLeftData }): State {


    // Remove o jogador da lista
    const newPlayers = state.players.filter(p => p.id !== action.data.player.id);

    // Atualiza estado do lobby se estava FULL
    if (state.lobby?.state === "FULL") {
        const updatedLobby: Lobby = { ...state.lobby, state: "OPEN" };
        return { ...state, players: newPlayers, lobby: updatedLobby };
    }

    return { ...state, players: newPlayers };
}

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
                const [lobby, host, players] = await Promise.all([
                    api.getLobbyById(Number(lobbyId)),
                    api.getLobbyHost(Number(lobbyId)),
                    api.getLobbyPlayers(Number(lobbyId)),
                ]);


                dispatch({ type: "set-lobby", lobby, host, players });
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

    // SSE listener
    const handleSSEMessage = useCallback((message: LobbySSEMessage) => {

        switch (message.action) {
            case "player-joined":
                dispatch({ type: "player-join", data: message.data as playerJoinedData });
                break;
            case "player-left":;
                dispatch({ type: "player-left", data: message.data as playerLeftData });
                break;
            case "match-starting":
                dispatch({ type: "match-starting", data: message.data as matchStartingData });
                break;

        }
    }, []);

    useLobbyListener(lobbyId ? Number(lobbyId) : undefined, handleSSEMessage);

    const handleJoin = async () => {
        if (!lobbyId) return;

        dispatch({ type: "joining" });
        try {
            const result = await api.joinLobby(Number(lobbyId));
            dispatch({ type: "action-complete" });

            if (result.matchId) {
                dispatch({ type: "match-starting", data: { matchId: result.matchId } });
                navigate(`/matches/${result.matchId}/events`);
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

    const { lobby, host, players } = state;
    const isInLobby = user && players.some(p => p.id === user.id);
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
                        {players.map((player) => (
                            <li key={player.id} className="lobby-player-item">
                                {player.nickname}
                                {player.id === lobby.hostUserId && (
                                    <span className="lobby-host-badge">Host</span>
                                )}
                                {user && player.id === user.id && (
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
