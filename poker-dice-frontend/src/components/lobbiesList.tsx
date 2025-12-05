import { Link } from "react-router";
import { Lobby } from "../types";
import { api, ApiError } from "../api";
import { useGlobalLobbyEvents, LobbySSEMessage, LobbyCreatedData, LobbyDeletedData } from "../hooks/useGlobalLobbyEvents";
import { useState, useEffect } from "react";
import "../styles/App.css";

// Estado inicial
const initialState = {
    lobbies: [] as Lobby[],
    lobbyPlayersCount: {} as { [lobbyId: number]: number },
    isLoading: true,
    error: null as string | null,
};

// Função auxiliar para lidar com lobby criado
function handleLobbyCreated(
    lobbies: Lobby[],
    lobbyPlayersCount: { [lobbyId: number]: number },
    data: LobbyCreatedData
): { lobbies: Lobby[]; lobbyPlayersCount: { [lobbyId: number]: number } } {
    return {
        lobbies: [...lobbies, data.Lobby],
        lobbyPlayersCount: { ...lobbyPlayersCount, [data.Lobby.id]: 0 },
    };
}

// Função auxiliar para lidar com lobby apagado
function handleLobbyDeleted(
    lobbies: Lobby[],
    lobbyPlayersCount: { [lobbyId: number]: number },
    data: LobbyDeletedData
): { lobbies: Lobby[]; lobbyPlayersCount: { [lobbyId: number]: number } } {
    const updatedPlayersCount = { ...lobbyPlayersCount };
    delete updatedPlayersCount[data.LobbyId];

    return {
        lobbies: lobbies.filter((lobby) => lobby.id !== data.LobbyId),
        lobbyPlayersCount: updatedPlayersCount,
    };
}

// Função para carregar lobbies e número de jogadores
async function loadLobbiesData(
    setLobbies: (lobbies: Lobby[]) => void,
    setLobbyPlayersCount: (count: { [lobbyId: number]: number }) => void,
    setIsLoading: (loading: boolean) => void,
    setError: (error: string | null) => void
) {
    setIsLoading(true);
    try {
        const fetchedLobbies = await api.getAllLobbies();
        setLobbies(fetchedLobbies);

        // Fetch número de jogadores para cada lobby
        const playersCountPromises = fetchedLobbies.map(async (lobby) => {
            try {
                const players = await api.getLobbyPlayers(lobby.id);
                return { lobbyId: lobby.id, count: players.length };
            } catch (err) {
                console.error(`Failed to fetch players for lobby ${lobby.id}`, err);
                return { lobbyId: lobby.id, count: 0 };
            }
        });

        const playersCountResults = await Promise.all(playersCountPromises);
        const playersCountMap = playersCountResults.reduce(
            (acc, { lobbyId, count }) => {
                acc[lobbyId] = count;
                return acc;
            },
            {} as { [lobbyId: number]: number }
        );

        setLobbyPlayersCount(playersCountMap);
        setError(null);
    } catch (err) {
        if (err instanceof ApiError) {
            setError(err.message);
        } else {
            setError("Failed to load lobbies");
        }
    } finally {
        setIsLoading(false);
    }
}

export function LobbiesList() {
    const [lobbies, setLobbies] = useState<Lobby[]>(initialState.lobbies);
    const [lobbyPlayersCount, setLobbyPlayersCount] = useState<{
        [lobbyId: number]: number;
    }>(initialState.lobbyPlayersCount);
    const [isLoading, setIsLoading] = useState(initialState.isLoading);
    const [error, setError] = useState<string | null>(initialState.error);

    // Carrega dados iniciais
    useEffect(() => {
        loadLobbiesData(setLobbies, setLobbyPlayersCount, setIsLoading, setError);
    }, []);

    // SSE setup
    useGlobalLobbyEvents((message: LobbySSEMessage) => {
        console.log("SSE Message received:", message);

        switch (message.action) {
            case "lobby-created": {
                const data = message.data as LobbyCreatedData;
                const updated = handleLobbyCreated(lobbies, lobbyPlayersCount, data);
                setLobbies(updated.lobbies);
                setLobbyPlayersCount(updated.lobbyPlayersCount);
                break;
            }
            case "lobby-deleted": {
                const data = message.data as LobbyDeletedData;
                const updated = handleLobbyDeleted(lobbies, lobbyPlayersCount, data);
                setLobbies(updated.lobbies);
                setLobbyPlayersCount(updated.lobbyPlayersCount);
                break;
            }
        }
    });

    if (isLoading) {
        return <div className="lobbies-list-loading">Loading lobbies...</div>;
    }

    if (error) {
        return <div className="lobbies-list-error">{error}</div>;
    }

    return (
        <div className="lobbies-list-container">
            <h2>Available Lobbies</h2>
            {lobbies.length === 0 ? (
                <p className="lobbies-list-empty">
                    No lobbies found. Create one to get started!
                </p>
            ) : (
                <div className="lobbies-list-grid">
                    {lobbies.map((lobby) => {
                        const playersCount = lobbyPlayersCount[lobby.id] ?? 0;
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
                      Players: {playersCount}/{lobby.maxPlayers}
                    </span>
                                        <span>Rounds: {lobby.rounds}</span>
                                        <span>Ante: {lobby.ante}€</span>
                                        <span
                                            className={`lobby-state lobby-state-${lobby.state.toLowerCase()}`}
                                        >
                      {lobby.state === "OPEN"
                          ? "Open"
                          : lobby.state === "FULL"
                              ? "Closed"
                              : "In Game"}
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
