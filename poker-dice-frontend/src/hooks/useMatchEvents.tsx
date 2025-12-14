import { useEffect, useRef } from "react";
import {Face, Wallet} from "../types.ts";


type gameActions = "match-snapshot" | "turn-change" | "round-complete" | "game-end" | "dice-rolled" | "dice-held";

export interface TurnChangeEvent {
  previousPlayer: number;
  currentPlayer: number;
}


export type RoundSummaryEvent = {
  roundNumber: number;
  winners: number[];
  prize: number;
  wallets: Record<number, Wallet>;
  playersAndCombinations: {
  [playerId: number]: {
    faces?: Face[];
    combination: string;
  };
  };

};

export interface MatchSnapshotEvent {
    matchId: number;
    currentRoundNumber: number;
    totalRounds: number;
    playerOrder: number[];
    currentPlayer: number;
}

export interface GameEndEvent {
    winner: number;
    wallets: Record<number, Wallet>;
}

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


export interface MatchSSEMessage {
  action: gameActions;
  data: TurnChangeEvent | RoundSummaryEvent | MatchSnapshotEvent | GameEndEvent | DiceRolledData | DiceHeldData;
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