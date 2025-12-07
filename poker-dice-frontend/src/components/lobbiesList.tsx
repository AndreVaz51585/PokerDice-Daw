import { Link } from "react-router";
import { Lobby } from "../types";
import { api, ApiError } from "../api";
import { useGlobalLobbiesListener, LobbySSEMessage, LobbyCreatedData, LobbyDeletedData } from "../hooks/useGlobalLobbyEvents";
import {useCallback, useEffect, useReducer} from "react";
import "../styles/App.css";


type State = {
    lobbies: Lobby[];
    isLoading: boolean;
    error: String | null;
};


type Action =
    | { type: "set-lobbies"; lobbies: Lobby[] }
    | { type: "add-lobby"; lobby: Lobby }
    | { type: "delete-lobby"; lobbyId: number }
    | { type: "load"; isLoading: boolean }
    | { type: "error"; error: string | null };


function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "set-lobbies":
            return {
                ...state,
                lobbies: action.lobbies,
                isLoading: false,
                error: null,
            };
        case "add-lobby":
            return {
                ...state,
                lobbies: [...state.lobbies, action.lobby],
            };
        case "delete-lobby":
            return {
                ...state,
                lobbies: state.lobbies.filter((lobby) => lobby.id !== action.lobbyId),
            };
        case "load":
            return { ...state, isLoading: action.isLoading };
        case "error":
            return { ...state, error: action.error, isLoading: false };
        default:
            return state;
    }
}

// Initial state
const initialState: State = {
    lobbies: [],
    isLoading: true,
    error: null,
};


async function loadLobbiesData(
    dispatch: React.Dispatch<Action>
) {
    dispatch({ type: "load", isLoading: true });
    try {
        const data: Lobby[] = await api.getAllLobbies();
        dispatch({
            type: "set-lobbies",
            lobbies: data,
        });
    } catch (err) {
        if (err instanceof ApiError) {
            dispatch({ type: "error", error: err.message });
        } else {
            dispatch({ type: "error", error: "Failed to load Lobbies" });
        }
    }
}



export function LobbiesList() {
    const [state, dispatch] = useReducer(reducer, initialState);

    // Carrega dados iniciais
    useEffect(() => {
        loadLobbiesData(dispatch);
    }, []);

    const handleSSEMessage = useCallback((message: LobbySSEMessage) => {
        console.log("SSE Message received:", message);

        switch (message.action) {
            case "lobby-created": {
                const data = message.data as LobbyCreatedData;
                dispatch({ type: "add-lobby", lobby: data });
                break;
            }
            case "lobby-deleted": {
                const data = message.data as LobbyDeletedData;
                dispatch({ type: "delete-lobby", lobbyId: data.LobbyId });
                break;
            }
        }
    }, []);



    // SSE setup
    useGlobalLobbiesListener(handleSSEMessage);


    if (state.isLoading) {
        return <div className="lobbies-list-loading">Loading lobbies...</div>;
    }

    if (state.error) {
        return <div className="lobbies-list-error">{state.error}</div>;
    }

    return (
        <div className="lobbies-list-container">
            <h2>Available Lobbies</h2>
            {state.lobbies.length === 0 ? (
                <p className="lobbies-list-empty">
                    No lobbies found. Create one to get started!
                </p>
            ) : (
                <div className="lobbies-list-grid">
                    {state.lobbies.map((lobby) => {
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
