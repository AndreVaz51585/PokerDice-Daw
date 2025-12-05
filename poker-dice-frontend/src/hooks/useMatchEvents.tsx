import { useEffect, useRef } from "react";

export interface SSEMessage {
  eventType: string;
  eventId: string;
  data: any;
}

export function useMatchEvents(
  matchId: string | undefined,
  onMessage: (message: SSEMessage) => void
) {
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!matchId) return;

    // Note: backend exposes /api/matches/{matchId}/events (no "sse" path)
    const url = `/api/matches/${matchId}/events`;
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;

    const eventTypes = [
      "match-snapshot",
      "turn-change",
      "round-complete",
      "game-end",
    ];

    const listeners: { type: string; handler: (ev: MessageEvent) => void }[] = [];

    const makeHandler = (eventType: string) => (ev: MessageEvent) => {
      try {
        const parsed = JSON.parse(ev.data);
        onMessage({
          eventType,
          eventId: ev.lastEventId || "",
          data: parsed,
        });
      } catch (err) {
        // If parsing fails, forward raw string as data
        onMessage({
          eventType,
          eventId: ev.lastEventId || "",
          data: ev.data,
        });
      }
    };

    eventTypes.forEach((et) => {
      const h = makeHandler(et);
      eventSource.addEventListener(et, h as EventListener);
      listeners.push({ type: et, handler: h });
    });

    // also listen generic `message` frames
    const genericHandler = (ev: MessageEvent) => {
      try {
        const payload = JSON.parse(ev.data);
        onMessage({ eventType: "message", eventId: ev.lastEventId || "", data: payload });
      } catch {
        onMessage({ eventType: "message", eventId: ev.lastEventId || "", data: ev.data });
      }
    };

    eventSource.addEventListener("message", genericHandler as EventListener);

    eventSource.onerror = (err) => {
      console.error("SSE connection error (match):", err);
    };

    return () => {
      listeners.forEach((l) => eventSource.removeEventListener(l.type, l.handler as EventListener));
      eventSource.removeEventListener("message", genericHandler as EventListener);
      eventSource.close();
      eventSourceRef.current = null;
    };
  }, [matchId, onMessage]);

  return eventSourceRef;
}