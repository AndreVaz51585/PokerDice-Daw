import {useEffect, useRef} from "react";
import {Lobby} from "../types.ts";

type LobbiesUpdateAction = "lobby-created" | "lobby-deleted";

export type LobbyCreatedData = Lobby


export interface LobbyDeletedData {
    LobbyId: number;
    message: string
}

export interface LobbySSEMessage {
    action: LobbiesUpdateAction;
    data: LobbyCreatedData | LobbyDeletedData;
}

export function useGlobalLobbiesListener(
    onMessage: (message: LobbySSEMessage) => void
) {
    const onMessageRef = useRef(onMessage);

    useEffect(() => {
        onMessageRef.current = onMessage;
    }, [onMessage]);

    useEffect(() => {
        const eventSource = new EventSource(`/api/lobbies/events`);

        eventSource.addEventListener("lobby-created", (event) => {
            try {
                const lobby = JSON.parse(event.data);
                const message: LobbySSEMessage = {
                    action: "lobby-created",
                    data: lobby
                };
                onMessageRef.current(message);
            } catch (error) {

            }
        });

        eventSource.addEventListener("lobby-deleted", (event) => {
            try {
                const data = JSON.parse(event.data);
                const message: LobbySSEMessage = {
                    action: "lobby-deleted",
                    data: {LobbyId: data.lobbyId, message: data.message || ""}
                };
                onMessageRef.current(message);
            } catch (error) {
                console.error("Error parsing lobby-deleted:", error);
            }
        });

        eventSource.onerror = (error) => {
            console.error("SSE Error:", error);
            console.error("readyState:", eventSource.readyState);
        };

        return () => {
            eventSource.close();
        };
    }, []);
}