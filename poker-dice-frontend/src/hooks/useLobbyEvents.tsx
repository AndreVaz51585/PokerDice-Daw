import {useEffect, useRef} from "react";
import { Player} from "../types.ts";

type LobbyUpdateAction = "player-joined" | "player-left"| "match-starting" ; // para já definir apenas estes dois posteriormente quem for tratar do matchStarted deverá decidir qual o tratamento


export interface playerJoinedData {
     player : Player,
     currentCount : number,
     maxPlayers : number
}

export interface playerLeftData {
     player : Player,
     currentCount : number,
}

export interface matchStartingData {
    matchId: number;
}



export interface LobbySSEMessage {
    action: LobbyUpdateAction;
    data: playerJoinedData | playerLeftData | matchStartingData;
}

export function useLobbyListener(
    lobbyId: number | undefined,
    onMessage: (message: LobbySSEMessage) => void
) {
    const onMessageRef = useRef(onMessage);

    useEffect(() => {
        onMessageRef.current = onMessage;
    }, [onMessage]);

    useEffect(() => {
        if (!lobbyId) return;

        const eventSource = new EventSource(`/api/lobbies/${lobbyId}/events`);

        // Ouve eventos nomeados
        eventSource.addEventListener("player-joined", (event) => {
            try {
                const data = JSON.parse(event.data) as playerJoinedData;
                onMessageRef.current({
                    action: "player-joined",
                    data
                });
            } catch (error) {
                console.error("Parse error (player-joined):", error);
            }
        });

        eventSource.addEventListener("player-left", (event) => {
            try {
                const data = JSON.parse(event.data) as playerLeftData;
                onMessageRef.current({
                    action: "player-left",
                    data
                });
            } catch (error) {
                console.error("Parse error (player-left):", error);
            }
        });

        eventSource.addEventListener("match-starting", (event) => {
            try {
                const data = JSON.parse(event.data) as matchStartingData;
                onMessageRef.current({
                    action: "match-starting",
                    data
                });
            } catch (error) {
                console.error("Parse error (player-left):", error);
            }
        });

        eventSource.onerror = (error) => {
            console.error("SSE Error:", error);
        };

        return () => {
            eventSource.close();
        };
    }, [lobbyId]);
}