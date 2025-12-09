import { useEffect, useRef } from "react";
import { Face } from "../types";

type DiceEventAction = "dice-rolled" | "dice-held";

export interface DiceRolledData {
    userId: number;
    dice: Face[];
    rerollsLeft: number;
    combination: string;
}

export interface DiceHeldData {
    userId: number;
    heldIndices: number[];
}

export interface DiceSSEMessage {
    action: DiceEventAction;
    data: DiceRolledData | DiceHeldData;
}

export function useMatchDiceEvents(
    matchId: number | undefined,
    onMessage: (message: DiceSSEMessage) => void
) {
    const onMessageRef = useRef(onMessage);

    useEffect(() => {
        onMessageRef.current = onMessage;
    }, [onMessage]);

    useEffect(() => {
        if (!matchId) return;

        const eventSource = new EventSource(`/api/matches/${matchId}/events`);

        eventSource.addEventListener("dice-rolled", (event) => {
            try {
                const data = JSON.parse(event.data) as DiceRolledData;
                onMessageRef.current({
                    action: "dice-rolled",
                    data
                });
            } catch (error) {
                console.error("Parse error (dice-rolled):", error);
            }
        });

        eventSource.addEventListener("dice-held", (event) => {
            try {
                const data = JSON.parse(event.data) as DiceHeldData;
                onMessageRef.current({
                    action: "dice-held",
                    data
                });
            } catch (error) {
                console.error("Parse error (dice-held):", error);
            }
        });

        eventSource.onerror = (error) => {
            console.error("SSE Error:", error);
        };

        return () => {
            eventSource.close();
        };
    }, [matchId]);
}
