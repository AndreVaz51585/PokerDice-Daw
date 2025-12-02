import { useCallback, useReducer } from "react";
import { useParams } from "react-router";
import { api, ApiError } from "../api";
import { MatchSnapshot, Face, PlayerState } from "../types";
import { useAuth } from "../AuthContext";
import { useMatchEvents, SSEMessage } from "../hooks/useMatchEvents";
import "../styles/App.css";

// State type
type State = {
  matchState: MatchSnapshot | null;
  selectedDice: Set<number>;
  error: string | null;
  isLoading: boolean;
  isRolling: boolean;
  isHolding: boolean;
  isFinishing: boolean;
};

// Action types
type Action =
  | { type: "set-match-state"; state: MatchSnapshot }
  | { type: "toggle-dice"; index: number }
  | { type: "clear-selection" }
  | { type: "error"; error: string }
  | { type: "rolling" }
  | { type: "holding" }
  | { type: "finishing" }
  | { type: "action-complete" }
  | { type: "set-loading"; isLoading: boolean };

// Reducer function
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "set-match-state":
      return {
        ...state,
        matchState: action.state,
        isLoading: false,
        isRolling: false,
        isHolding: false,
        isFinishing: false,
      };
    case "toggle-dice":
      const newSelected = new Set(state.selectedDice);
      if (newSelected.has(action.index)) {
        newSelected.delete(action.index);
      } else {
        newSelected.add(action.index);
      }
      return { ...state, selectedDice: newSelected };
    case "clear-selection":
      return { ...state, selectedDice: new Set() };
    case "error":
      return {
        ...state,
        error: action.error,
        isRolling: false,
        isHolding: false,
        isFinishing: false,
      };
    case "rolling":
      return { ...state, isRolling: true, error: null };
    case "holding":
      return { ...state, isHolding: true, error: null };
    case "finishing":
      return { ...state, isFinishing: true, error: null };
    case "action-complete":
      return {
        ...state,
        isRolling: false,
        isHolding: false,
        isFinishing: false,
        selectedDice: new Set(),
      };
    case "set-loading":
      return { ...state, isLoading: action.isLoading };
    default:
      return state;
  }
}

// Initial state
const initialState: State = {
  matchState: null,
  selectedDice: new Set(),
  error: null,
  isLoading: true,
  isRolling: false,
  isHolding: false,
  isFinishing: false,
};

// Dice face emoji mapping
const faceEmoji: Record<Face, string> = {
  NINE: "9️⃣",
  TEN: "🔟",
  JACK: "🃏",
  QUEEN: "👸",
  KING: "🤴",
  ACE: "🅰️",
};

// Combination name mapping
const combinationNames: Record<string, string> = {
  FIVE_OF_A_KIND: "Five of a Kind 🎰",
  FOUR_OF_A_KIND: "Four of a Kind 🎲",
  FULL_HOUSE: "Full House 🏠",
  STRAIGHT: "Straight 📈",
  THREE_OF_A_KIND: "Three of a Kind 🎯",
  TWO_PAIR: "Two Pair 👥",
  ONE_PAIR: "One Pair 👤",
  BUST: "Bust 💔",
};

export function MatchView() {
  const { matchId } = useParams<{ matchId: string }>();
  const [state, dispatch] = useReducer(reducer, initialState);
  const { user } = useAuth();

  // Handle SSE messages - defensive parsing + validation
  const handleMessage = useCallback((message: SSEMessage) => {
    console.log("Received SSE message:", message.eventType, message.data);

    // message.data might be a string or already an object
    let payload: any = message.data;
    if (typeof payload === "string") {
      try {
        payload = JSON.parse(payload);
      } catch (e) {
        console.warn("Could not parse SSE payload:", e);
        return;
      }
    }

    // Basic validation: payload should have a 'game' object and 'wallets'
    if (!payload || typeof payload !== "object" || !payload.game) {
      console.warn("Invalid match snapshot payload (missing game):", payload);
      return;
    }

    // Ensure players is an array (defensive)
    if (!Array.isArray(payload.game.players)) {
      payload.game.players = [];
    }

    // Ensure wallets is an object
    if (!payload.wallets || typeof payload.wallets !== "object") {
      payload.wallets = {};
    }

    // Finally dispatch as MatchSnapshot
    dispatch({ type: "set-match-state", state: payload as MatchSnapshot });
  }, []);

  // Set up SSE connection
  useMatchEvents(matchId, handleMessage);

  const handleRoll = async () => {
    if (!matchId) return;

    dispatch({ type: "rolling" });
    try {
      await api.sendCommand(Number(matchId), { type: "ROLL" });
      dispatch({ type: "action-complete" });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", error: err.message });
      } else {
        dispatch({ type: "error", error: "Erro ao lançar os dados" });
      }
    }
  };

  const handleHold = async () => {
    if (!matchId) return;

    dispatch({ type: "holding" });
    try {
      await api.sendCommand(Number(matchId), {
        type: "HOLD",
        indices: Array.from(state.selectedDice),
      });
      dispatch({ type: "action-complete" });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", error: err.message });
      } else {
        dispatch({ type: "error", error: "Erro ao guardar os dados" });
      }
    }
  };

  const handleFinishTurn = async () => {
    if (!matchId) return;

    dispatch({ type: "finishing" });
    try {
      await api.sendCommand(Number(matchId), { type: "FINISH_TURN" });
      dispatch({ type: "action-complete" });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", error: err.message });
      } else {
        dispatch({ type: "error", error: "Erro ao terminar a jogada" });
      }
    }
  };

  if (state.isLoading && !state.matchState) {
    return <div className="match-loading">A carregar partida...</div>;
  }

  if (!state.matchState) {
    return <div className="match-error">Partida não encontrada</div>;
  }

  // Defensive defaults for nested structures
  const matchState = state.matchState;
  const game = matchState.game ?? ({
    phase: "FINISHED",
    rounds: [],
    currentRoundIndex: 0,
    ante: 0,
    players: [],
  } as typeof matchState.game);
  const players = Array.isArray(game.players) ? game.players : ([] as PlayerState[]);
  const wallets = matchState.wallets ?? ({} as Record<number, any>);

  const currentPlayer = players.find(
    (p: PlayerState) => p.userId === matchState.currentPlayerId
  );
  const myPlayer = user ? players.find((p: PlayerState) => p.userId === user.id) : null;
  const isMyTurn = user && matchState.currentPlayerId === user.id;
  const myWallet = user ? wallets[user.id] : null;
  const gameFinished = game.phase === "FINISHED";

  return (
    <div className="match-container">
      <div className="match-header">
        <h2> Partida #{matchId}</h2>
        <div className="match-info">
          <span>Ronda: {(game.currentRoundIndex ?? 0) + 1}</span>
          <span>Ante: {game.ante ?? 0}€</span>
          <span className={`match-phase match-phase-${(game.phase ?? "FINISHED").toLowerCase()}`}>
            {game.phase === "LOBBY"
              ? "A esperar"
              : game.phase === "ROLLING"
              ? "Em progresso"
              : "Terminado"}
          </span>
        </div>
      </div>

      {state.error && <div className="match-error-message">{state.error}</div>}

      {/* Game finished message */}
      {gameFinished && (
        <div className="match-finished">
          <h3> Jogo Terminado!</h3>
          <p>Obrigado por jogar.</p>
        </div>
      )}

      {/* Current turn indicator */}
      {!gameFinished && currentPlayer && (
        <div className={`match-turn-indicator ${isMyTurn ? "my-turn" : ""}`}>
          {isMyTurn ? (
            <span> É a sua vez de jogar!</span>
          ) : (
            <span>
              A esperar por Jogador #{currentPlayer.userId}...
            </span>
          )}
        </div>
      )}

      {/* My hand and controls */}
      {myPlayer && !gameFinished && (
        <div className="match-my-hand">
          <h3>A Sua Mão</h3>
          {myWallet && (
            <div className="match-my-balance">
              Saldo: {myWallet.currentBalance}€
            </div>
          )}

          <div className="match-dice-container">
            {(myPlayer.hand?.dices ?? []).map((dice, index) => (
              <div
                key={index}
                className={`match-dice ${
                  state.selectedDice.has(index) ? "selected" : ""
                } ${(myPlayer.heldIndices ?? []).includes(index) ? "held" : ""}`}
                onClick={() => {
                  if (isMyTurn && !(myPlayer.heldIndices ?? []).includes(index)) {
                    dispatch({ type: "toggle-dice", index });
                  }
                }}
              >
                <span className="dice-face">{faceEmoji[dice.value]}</span>
                <span className="dice-value">{dice.value}</span>
              </div>
            ))}
            {(myPlayer.hand?.dices ?? []).length === 0 && (
              <div className="match-no-dice">
                Clique em "Lançar Dados" para começar
              </div>
            )}
          </div>

          {myPlayer.combination && (
            <div className="match-combination">
              <strong>Combinação:</strong>{" "}
              {combinationNames[myPlayer.combination.type] ||
                myPlayer.combination.type}
            </div>
          )}

          <div className="match-roll-info">
            <span>Lançamentos restantes: {myPlayer.rollsRemaining}</span>
          </div>

          {/* Game controls */}
          {isMyTurn && (
            <div className="match-controls">
              {!myPlayer.hasRolled && myPlayer.rollsRemaining > 0 && (
                <button
                  onClick={handleRoll}
                  disabled={state.isRolling}
                  className="match-roll-btn"
                >
                  {state.isRolling ? "A lançar..." : " Lançar Dados"}
                </button>
              )}

              {myPlayer.hasRolled && myPlayer.rollsRemaining > 0 && (
                <>
                  <button
                    onClick={handleHold}
                    disabled={state.isHolding || state.selectedDice.size === 0}
                    className="match-hold-btn"
                  >
                    {state.isHolding
                      ? "A guardar..."
                      : ` Guardar (${state.selectedDice.size})`}
                  </button>
                  <button
                    onClick={handleRoll}
                    disabled={state.isRolling}
                    className="match-roll-btn"
                  >
                    {state.isRolling ? "A lançar..." : " Lançar Novamente"}
                  </button>
                </>
              )}

              {myPlayer.hasRolled && (
                <button
                  onClick={handleFinishTurn}
                  disabled={state.isFinishing}
                  className="match-finish-btn"
                >
                  {state.isFinishing ? "A terminar..." : " Terminar Jogada"}
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {/* All players */}
      <div className="match-players">
        <h3>Jogadores</h3>
        <div className="match-players-grid">
          {players.map((player: PlayerState) => {
            const wallet = wallets[player.userId];
            const isCurrentTurn = player.userId === matchState.currentPlayerId;
            const isMe = user && player.userId === user.id;

            return (
              <div
                key={player.userId}
                className={`match-player-card ${isCurrentTurn ? "current-turn" : ""} ${
                  isMe ? "is-me" : ""
                }`}
              >
                <div className="match-player-header">
                  <span className="match-player-name">
                    Jogador #{player.userId}
                    {isMe && " (Você)"}
                  </span>
                  {wallet && (
                    <span className="match-player-balance">
                      {wallet.currentBalance}€
                    </span>
                  )}
                </div>

                {player.hand && !isMe && (
                  <div className="match-player-dice">
                    {(player.hand.dices ?? []).map((dice, index) => (
                      <span
                        key={index}
                        className={`match-player-dice-item ${
                          (player.heldIndices ?? []).includes(index) ? "held" : ""
                        }`}
                      >
                        {faceEmoji[dice.value]}
                      </span>
                    ))}
                  </div>
                )}

                {player.combination && (
                  <div className="match-player-combination">
                    {combinationNames[player.combination.type] ||
                      player.combination.type}
                  </div>
                )}

                <div className="match-player-status">
                  {player.hasRolled ? (
                    <span className="status-rolled">Jogou</span>
                  ) : (
                    <span className="status-waiting">A esperar</span>
                  )}
                  <span className="rolls-remaining">
                    Rolls: {player.rollsRemaining}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}