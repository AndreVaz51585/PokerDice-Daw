import {useCallback, useEffect, useReducer} from "react";
import { useParams, useNavigate } from "react-router";
import { api, ApiError } from "../api";
import { DiceHoldResponse, DiceRollResponse, Face } from "../types";
import { useAuth } from "../AuthContext";
import {
  useMatchEvents,
  MatchSSEMessage,
  TurnChangeEvent,
  RoundSummaryEvent,
  MatchSnapshotEvent,
  GameEndEvent,
  DiceRolledData,
  DiceHeldData
} from "../hooks/useMatchEvents";

import "../styles/App.css";

import {RoundSummaryPanel} from "./RoundSummary";
import {useMatch} from "../MatchContext.tsx";

// State types
type PlayerState = {
  userId: number;
  dice: Face[];
  heldIndices: number[];
  rerollsLeft: number;
  combination: string | null;
};

type State = {
  matchId: number | null;
  currentRoundNumber: number;
  totalRounds: number
  playerOrder: number[];
  currentPlayer: number | null;
  players: Map<number, PlayerState>;
  selectedDice: Set<number>;
  isLoading: boolean;
  isRolling: boolean;
  isHolding: boolean;
  isFinishing: boolean;
  error: string | null;
  gameEnded: boolean;
  winner: number | null;
  lastRoundSummary: RoundSummaryEvent | null
};

// Action types
type Action =
    | { type: "set-snapshot"; data: MatchSnapshotEvent }
    | { type: "turn-change"; data: TurnChangeEvent }
    | { type: "round-complete"; data: RoundSummaryEvent }
    | { type: "game-end"; data: GameEndEvent, gameEnded: boolean }
    | { type: "dice-rolled"; data: DiceRolledData }
    | { type: "dice-held"; data: DiceHeldData }
    | { type: "update-my-roll"; userId: number; data: { dices: Face[]; rerollsLeft: number; hand: string } }  
    | { type: "update-my-hold"; userId: number; data: { dices: Face[]; heldIndices: number[]; rerollsLeft: number } }  
    | { type: "toggle-dice"; index: number }
    | { type: "clear-selection" }
    | { type: "rolling" }
    | { type: "holding" }
    | { type: "finishing" }
    | { type: "action-complete" }
    | { type: "error"; error: string }


// Reducer
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "set-snapshot":
      return {
        ...state,
        matchId: action.data.matchId,
        currentRoundNumber: action.data.currentRoundNumber,
        totalRounds : action.data.totalRounds,
        playerOrder: action.data.playerOrder,
        currentPlayer: action.data.currentPlayer,
        players: new Map(
            action.data.playerOrder.map(userId => [
              userId,
              state.players.get(userId) || {
                userId,
                dice: [],
                heldIndices: [],
                rerollsLeft: 3,
                combination: null
              }
            ])
        ),
        isLoading: false,
        error: null
      };

    case "turn-change":
      return {
        ...state,
        currentPlayer: action.data.currentPlayer,
        selectedDice: new Set()
      };

    case "round-complete":
  return {
    ...state,

    currentRoundNumber: action.data.roundNumber + 1,
    players: new Map(
      state.playerOrder.map(userId => [
        userId,
        {
          userId,
          dice: [],
          heldIndices: [],
          rerollsLeft: 3,
          combination: null
        }
      ])
    ),
    selectedDice: new Set(),
    lastRoundSummary: action.data
  };


    case "game-end":
      return {
        ...state,
        gameEnded: true,
        winner: action.data.winner,
        currentPlayer: null
      };

    case "dice-rolled": {
      const player = state.players.get(action.data.userId);
      if (!player) return state;

      const updatedPlayer: PlayerState = {
        ...player,
        dice: action.data.dice,
        rerollsLeft: action.data.rerollsLeft,
        combination: action.data.combination
      };

      const newPlayers = new Map(state.players);
      newPlayers.set(action.data.userId, updatedPlayer);

      return {
        ...state,
        players: newPlayers,
        isRolling: false,
        selectedDice: new Set()
      };
    }

    case "dice-held": {
      const player = state.players.get(action.data.userId);
      if (!player) return state;

      const updatedPlayer: PlayerState = {
        ...player,
        heldIndices: action.data.heldIndices
      };

      const newPlayers = new Map(state.players);
      newPlayers.set(action.data.userId, updatedPlayer);

      return {
        ...state,
        players: newPlayers,
        isHolding: false,
        selectedDice: new Set()
      };
    }

    case "update-my-roll": {
      const player = state.players.get(action.userId);
      if (!player) return state;

      const updatedPlayer: PlayerState = {
        ...player,
        dice: action.data.dices,
        rerollsLeft: action.data.rerollsLeft,
        combination: action.data.hand
      };

      const newPlayers = new Map(state.players);
      newPlayers.set(action.userId, updatedPlayer);

      return {
        ...state,
        players: newPlayers,
        isRolling: false,
        selectedDice: new Set(),
        error: null
      };
    }

    case "update-my-hold": {
      const player = state.players.get(action.userId);
      if (!player) return state;

      const updatedPlayer: PlayerState = {
        ...player,
        dice: action.data.dices,
        heldIndices: action.data.heldIndices,
        rerollsLeft: action.data.rerollsLeft
      };

      const newPlayers = new Map(state.players);
      newPlayers.set(action.userId, updatedPlayer);

      return {
        ...state,
        players: newPlayers,
        isHolding: false,
        selectedDice: new Set(),
        error: null
      };
    }


    case "toggle-dice": {
      const newSelectedDice = new Set(state.selectedDice);
      if (newSelectedDice.has(action.index)) {
        newSelectedDice.delete(action.index);
      } else {
        newSelectedDice.add(action.index);
      }
      return {
        ...state,
        selectedDice: newSelectedDice
      };
    }

    case "clear-selection":
      return { ...state, selectedDice: new Set() };

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
        error: null
      };

    case "error":
      return {
        ...state,
        isRolling: false,
        isHolding: false,
        isFinishing: false,
        error: action.error
      };

    default:
      return state;
  }
}

// Initial state
const initialState: State = {
  matchId: null,
  currentRoundNumber: 1,
  playerOrder: [],
  currentPlayer: null,
  totalRounds : 0,
  players: new Map(),
  selectedDice: new Set(),
  isLoading: true,
  isRolling: false,
  isHolding: false,
  isFinishing: false,
  error: null,
  gameEnded: false,
  winner: null,
  lastRoundSummary: null
};

// Face emoji mapping
const faceEmoji: Record<Face, string> = {
  NINE: "9",
  TEN: "10",
  JACK: "J",
  QUEEN: "Q",
  KING: "K",
  ACE: "A"
};

export function MatchView() {
  const { matchId } = useParams<{ matchId: string }>();
  const [state, dispatch] = useReducer(reducer, initialState);
  const { setCurrentMatch, clearCurrentMatch } = useMatch();
  const { user } = useAuth();
  const navigate = useNavigate();


    useEffect(() => {
        if (matchId) {
            console.log("✅ setCurrentMatch chamado com:", matchId);
            setCurrentMatch(matchId);
        }
    }, [matchId, setCurrentMatch]);


    useEffect(() => {
        if (state.gameEnded) {
            clearCurrentMatch();
        }
    }, [state.gameEnded, clearCurrentMatch]);


  // Handler para eventos gerais do match
  const handleMatchMessage = useCallback((message: MatchSSEMessage) => {
    switch (message.action) {
      case "match-snapshot":
        dispatch({ type: "set-snapshot", data: message.data as MatchSnapshotEvent });
        break;
      case "turn-change":
        dispatch({ type: "turn-change", data: message.data as TurnChangeEvent });
        break;
      case "round-complete":
        dispatch({ type: "round-complete", data: message.data as RoundSummaryEvent });
        break;
      case "game-end":
        dispatch({ type: "game-end", data: message.data as GameEndEvent , gameEnded : true});
        break;
      case "dice-rolled":
        dispatch({ type: "dice-rolled", data: message.data as DiceRolledData });
        break;
      case "dice-held":
        dispatch({ type: "dice-held", data: message.data as DiceHeldData });
        break;
    }
  }, []);


  // Subscrever eventos
  useMatchEvents(matchId, handleMatchMessage);

  // Ações do jogo
  const handleRoll = async () => {
    if (!matchId || !user) return;

    dispatch({ type: "rolling" });
    try {
      const response = await api.sendCommand(Number(matchId), { type: "roll" }) as DiceRollResponse;

      dispatch({
        type: "update-my-roll",
        userId: user.id,
        data: {
          dices: response.dices,
          rerollsLeft: response.rerollsLeft,
          hand: response.hand
        }
      });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", error: err.message });
      } else {
        dispatch({ type: "error", error: "Erro ao lançar dados" });
      }
    }
  };

  const handleHold = async () => {
    if (!matchId || !user || state.selectedDice.size === 0) return;

    const indicesToHold = Array.from(state.selectedDice);
    dispatch({ type: "holding" });
      try {
      const response = await api.sendCommand(Number(matchId), {
        type: "hold",
        indices: indicesToHold
      }) as DiceHoldResponse;

      dispatch({
        type: "update-my-hold",
        userId: user.id,
        data: {
          dices: response.dices,
          heldIndices: response.heldIndices,
          rerollsLeft: response.rerollsLeft
        }
      });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", error: err.message });
      } else {
        dispatch({ type: "error", error: "Erro ao segurar dados" });
      }
    }
  };

  const handleFinishTurn = async () => {
    if (!matchId) return;

    dispatch({ type: "finishing" });
    try {
      await api.sendCommand(Number(matchId), { type: "finish-turn" });
      dispatch({ type: "action-complete" });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", error: err.message });
      } else {
        dispatch({ type: "error", error: "Erro ao terminar turno" });
      }
    }
  };

  // UI
  if (state.isLoading) {
    return <div className="match-loading">A carregar partida...</div>;
  }

  if (!state.matchId) {
    return <div className="match-error">Partida não encontrada</div>;
  }

  const isMyTurn = user && state.currentPlayer === user.id;
  const myPlayer = user ? state.players.get(user.id) : null;

  if (state.gameEnded) {
    const winnerName = state.winner === user?.id ? "Você" : `Jogador ${state.winner}`;
    return (
        <div className="match-container">
          <div className="match-finished">
            <h2> Game Over!</h2>
            <p>Winner: {winnerName}</p>
            <button onClick={() => navigate("/")}>Back to Lobbies</button>
          </div>
        </div>
    );
  }

  return (
      <div className="match-container">
        <div className="match-header">
          <h2>Match #{state.matchId}</h2>
          <div className="match-info">
            <span>Round: {state.currentRoundNumber} / {state.totalRounds}</span>
            <span className={isMyTurn ? "your-turn" : ""}>
            {isMyTurn ? "It's your turn!" : `Player ${state.currentPlayer}'s turn`}
          </span>
          </div>
        </div>


        {state.lastRoundSummary && (
        <div className="match-round-summary-wrapper">
          <RoundSummaryPanel 
          summary={state.lastRoundSummary}
          />
        </div>
        )}

        {state.error && (
            <div className="match-error-message">{state.error}</div>
        )}

        {/* Minha mão */}
        {myPlayer && (
            <div className="match-my-hand">
              <h3>Your Hand</h3>

              <div className="match-dice-container">
                {myPlayer.dice.map((face, index) => (
                    <div
                        key={index}
                        className={`match-dice ${
                            state.selectedDice.has(index) ? "selected" : ""
                        } ${
                            myPlayer.heldIndices.includes(index) ? "held" : ""
                        }`}
                        onClick={() => {
                          if (isMyTurn && !myPlayer.heldIndices.includes(index)) {
                            dispatch({ type: "toggle-dice", index });
                          }
                        }}
                    >
                      <span className="dice-face">{faceEmoji[face]}</span>
                    </div>
                ))}
              </div>

              {myPlayer.combination && (
                  <div className="match-combination">
                    <strong>Combination:</strong> {myPlayer.combination}
                  </div>
              )}

              <div className="match-roll-info">
                <span>Rolls left: {myPlayer.rerollsLeft}</span>
              </div>

              {/* Controls */}
              {isMyTurn && (
                  <div className="match-controls">
                    <button
                        onClick={handleRoll}
                        disabled={state.isRolling || myPlayer.rerollsLeft === 0}
                        className="match-roll-btn"
                    >
                      {state.isRolling ? "Rolling..." : "Roll Dices"}
                    </button>

                    {myPlayer.dice.length > 0 && (
                        <>
                          <button
                              onClick={handleHold}
                              disabled={state.isHolding || state.selectedDice.size === 0}
                              className="match-hold-btn"
                          >
                            {state.isHolding ? "Holding..." : `Hold (${state.selectedDice.size})`}
                          </button>

                          <button
                              onClick={handleFinishTurn}
                              disabled={state.isFinishing}
                              className="match-finish-btn"
                          >
                            {state.isFinishing ? "Finishing..." : "Finish Turn"}
                          </button>
                        </>
                    )}
                  </div>
              )}
            </div>
        )}

        {/* Todos os jogadores */}
        <div className="match-players">
          <h3>Players</h3>
          <div className="match-players-grid">
            {state.playerOrder.map(userId => {
              const player = state.players.get(userId);
              if (!player) return null;

              const isCurrentTurn = userId === state.currentPlayer;
              const isMe = user && userId === user.id;

              return (
                  <div
                      key={userId}
                      className={`match-player-card ${isCurrentTurn ? "current-turn" : ""} ${isMe ? "is-me" : ""}`}
                  >
                    <div className="match-player-header">
                      <span>Player #{isMe} {isMe && "(You)"}</span>
                    </div>

                    {player.dice.length > 0 && !isMe && (
                        <div className="match-player-dice">
                          {player.dice.map((face, idx) => (
                              <span
                                  key={idx}
                                  className={player.heldIndices.includes(idx) ? "held" : ""}
                              >
                        {faceEmoji[face]}
                      </span>
                          ))}
                        </div>
                    )}

                    {player.combination && (
                        <div className="match-player-combination">
                          {player.combination}
                        </div>
                    )}

                    <div className="match-player-status">
                      <span>Rolls left: {player.rerollsLeft}</span>
                    </div>
                  </div>
              );
            })}
          </div>
        </div>
      </div>
  );
}

export default MatchView;
