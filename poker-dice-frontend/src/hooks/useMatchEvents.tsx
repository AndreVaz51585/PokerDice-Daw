import { useEffect, useRef } from "react";
import {Hand, Wallet} from "../types.ts";

//{}

type gameActions = "match-snapshot" | "turn-change" | "round-complete" | "game-end" ;

export interface TurnChangeEvent {
  previousPlayer: number;
  currentPlayer: number;
}

export interface RoundSummaryEvent {
    roundNumber: number;
    winners: number[] | null;
    prize: number;
    wallets: Record<number, Wallet>; // Replace 'any' with actual Wallet type
    playersAndCombinations: Record<number, Hand> | null; // Replace 'any' with actual Hand type
}

export interface MatchSnapshotEvent {
    matchId: number;
    currentRoundNumber: number;
    playerOrder: number[];
    currentPlayer: number;
}

export interface GameEndEvent {
    winner: number;
    prize: number;
    wallets: Record<number, Wallet>; // Replace 'any' with actual Wallet type
}


export interface MatchSSEMessage {
  action: gameActions;
  data: TurnChangeEvent | RoundSummaryEvent | MatchSnapshotEvent | GameEndEvent;
}

export function useMatchEvents(
  matchId: string | undefined,
  onMessage: (message: MatchSSEMessage) => void
) {
  const onMessageRef = useRef(onMessage);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!matchId) return;

    const eventSource = new EventSource(`/api/matches/${matchId}/events`);

    // Ouve eventos nomeados
    eventSource.addEventListener("match-snapshot", (event) => {
      try {
        const data = JSON.parse(event.data) as MatchSnapshotEvent;
        onMessageRef.current({
          action: "match-snapshot",
          data
        });
      } catch (error) {
        console.error("Parse error (match-snapshot):", error);
      }
    });

    eventSource.addEventListener("turn-change", (event) => {
      try {
        const data = JSON.parse(event.data) as TurnChangeEvent;
        onMessageRef.current({
          action: "turn-change",
          data
        });
      } catch (error) {
        console.error("Parse error (turn-change):", error);
      }
    });

    eventSource.addEventListener("round-complete", (event) => {
      try {
        const data = JSON.parse(event.data) as RoundSummaryEvent;
        onMessageRef.current({
          action: "round-complete",
          data
        });
      } catch (error) {
        console.error("Parse error (round-complete):", error);
      }
    });

    eventSource.addEventListener("game-end", (event) => {
      try {
        const data = JSON.parse(event.data) as GameEndEvent;
        onMessageRef.current({
          action: "game-end",
          data
        });
      } catch (error) {
        console.error("Parse error (game-end):", error);
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