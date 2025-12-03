import { useEffect, useRef } from "react";
import { MatchSnapshot } from "../types";

export interface SSEMessage {
  eventType: string;
  eventId: string;
  data: MatchSnapshot;
}

export function useMatchEvents(
  matchId: string | undefined,
  onMessage: (message: SSEMessage) => void
) {
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!matchId) return;

    const url = `/api/matches/sse/${matchId}/events`;
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;

    // Handle different event types
    const eventTypes = [
      "match-snapshot",
      "dice-rolled",
      "dice-held",
      "turn-change",
      "round-complete",
      "game-end",
    ];

    eventTypes.forEach((eventType) => {
      eventSource.addEventListener(eventType, (event) => {
        try {
          const data = JSON.parse(event.data);
          onMessage({
            eventType,
            eventId: event.lastEventId || "",
            data,
          });
        } catch (err) {
          console.error(`Error parsing SSE message for ${eventType}:`, err);
        }
      });
    });

    eventSource.onerror = (error) => {
      console.error("SSE connection error:", error);
      // Reconnection is handled automatically by EventSource
    };

    return () => {
      eventSource.close();
      eventSourceRef.current = null;
    };
  }, [matchId, onMessage]);

  return eventSourceRef;
}