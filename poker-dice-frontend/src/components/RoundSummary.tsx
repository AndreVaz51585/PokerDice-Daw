import { Face } from "../types";
import { RoundSummaryEvent } from "../hooks/useMatchEvents";

const faceMap: Record<Face, string> = {
  NINE: "9",
  TEN: "10",
  JACK: "J",
  QUEEN: "Q",
  KING: "K",
  ACE: "A",
};

type Props = {
  summary: RoundSummaryEvent;
};

export function RoundSummaryPanel({ summary }: Props) {
  const { wallets, playersAndCombinations, winners, roundNumber } = summary;

  return (
    <div className="round-summary-panel">
      <h3>Round Summary {roundNumber}</h3>

      <div className="round-summary-grid">
        {Object.keys(wallets).map((idStr) => {
          const playerId = Number(idStr);
          const wallet = wallets[playerId];
          const hand = playersAndCombinations[playerId];
          const isWinner = winners.includes(playerId);

          const faces = hand.faces;

          return (
            <div
              key={playerId}
              className={`round-summary-column ${isWinner ? "winner" : ""}`}
            >
              <div className="round-summary-player">
                Player #{playerId}
                {isWinner && <span className="winner-badge">WINNER</span>}
              </div>

              <div className="dice-row">
                {faces && faces.length === 5 ? (
                  faces.map((face, i) => (
                    <span key={i}>{faceMap[face]}</span>
                  ))
                ) : (
                  <span className="dice-empty">—</span>
                )}
              </div>

              <div className="hand-combination">
                {hand.combination}
              </div>

              <div className="round-summary-wallet">
                {wallet.currentBalance} €
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
