import { useEffect } from "react";
import {Lobby} from "../types.ts";

type LobbiesUpdateAction = "lobby-created" | "lobby-deleted";

export interface LobbyCreatedData {
    Lobby: Lobby;
}

export interface LobbyDeletedData {
    LobbyId: number;
    message: string
}

export interface LobbySSEMessage {
    action: LobbiesUpdateAction;
    data: LobbyCreatedData | LobbyDeletedData;
}

export function useGlobalLobbyEvents(
    onMessage: (message: LobbySSEMessage) => void
) {
    useEffect(() => {
        const eventSource = new EventSource(`/api/lobbies/events`);

        eventSource.addEventListener('lobby-created', (event: MessageEvent) => {
            const message: LobbySSEMessage = {
                action: 'lobby-created',
                data: JSON.parse(event.data),
            };
            onMessage(message);
        });

        eventSource.addEventListener('lobby-deleted', (event: MessageEvent) => {
            const message: LobbySSEMessage = {
                action: 'lobby-deleted',
                data: JSON.parse(event.data),
            };
            onMessage(message);
        });

        eventSource.onerror = (error) => {
            console.error("SSE Error:", error);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, [onMessage]);
}