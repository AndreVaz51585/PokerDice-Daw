import { useEffect, useReducer } from "react";
import { useParams } from "react-router";
import { api, ApiError } from "../api";
import { Statistics } from "../types";
import "../styles/App.css";

// State type
type State = {
  statistics: Statistics | null;
  isLoading: boolean;
  error: string | null;
};

// Action types
type Action =
  | { type: "load" }
  | { type: "set-statistics"; statistics: Statistics }
  | { type: "error"; error: string };

// Reducer function
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "load":
      return { ...state, isLoading: true, error: null };
    case "set-statistics":
      return { ...state, statistics: action.statistics, isLoading: false, error: null };
    case "error":
      return { ...state, isLoading: false, error: action.error };
    default:
      return state;
  }
}

// Initial state
const initialState: State = {
  statistics: null,
  isLoading: true,
  error: null,
};

export function StatisticsView() {
  const { userId } = useParams<{ userId: string }>();
  const [state, dispatch] = useReducer(reducer, initialState);

  // Load statistics data
  useEffect(() => {
    if (!userId) return;

    async function loadStatistics() {
      dispatch({ type: "load" });
      try {
        const statistics = await api.getStatistics(Number(userId));
        dispatch({ type: "set-statistics", statistics });
      } catch (err) {
        if (err instanceof ApiError) {
          dispatch({ type: "error", error: err.message });
        } else {
          dispatch({ type: "error", error: "Erro ao carregar estatísticas" });
        }
      }
    }

    loadStatistics();
  }, [userId]);

  if (state.isLoading) {
    return <div className="statistics-loading">A carregar estatísticas...</div>;
  }

  if (state.error || !state.statistics) {
    return (
      <div className="statistics-error">
        {state.error || "Estatísticas não encontradas"}
      </div>
    );
  }

  const { statistics } = state;
  const winRate =
    statistics.gamesPlayed > 0
      ? ((statistics.gamesWon / statistics.gamesPlayed) * 100).toFixed(1)
      : "0.0";

  return (
    <div className="statistics-container">
      <div className="statistics-card">
        <h2>📊 Estatísticas</h2>

        <div className="statistics-overview">
          <div className="statistics-overview-item">
            <span className="statistics-label">Jogos Jogados</span>
            <span className="statistics-value">{statistics.gamesPlayed}</span>
          </div>
          <div className="statistics-overview-item">
            <span className="statistics-label">Jogos Ganhos</span>
            <span className="statistics-value">{statistics.gamesWon}</span>
          </div>
          <div className="statistics-overview-item">
            <span className="statistics-label">Taxa de Vitória</span>
            <span className="statistics-value">{winRate}%</span>
          </div>
        </div>

        <h3>Combinações Obtidas</h3>
        <div className="statistics-combinations">
          <div className="statistics-combination">
            <span className="combination-name">🎰 Five of a Kind</span>
            <span className="combination-count">{statistics.fiveOfAKind}</span>
          </div>
          <div className="statistics-combination">
            <span className="combination-name">🎲 Four of a Kind</span>
            <span className="combination-count">{statistics.fourOfAKind}</span>
          </div>
          <div className="statistics-combination">
            <span className="combination-name">🏠 Full House</span>
            <span className="combination-count">{statistics.fullHouse}</span>
          </div>
          <div className="statistics-combination">
            <span className="combination-name">📈 Straight</span>
            <span className="combination-count">{statistics.straight}</span>
          </div>
          <div className="statistics-combination">
            <span className="combination-name">🎯 Three of a Kind</span>
            <span className="combination-count">{statistics.threeOfAKind}</span>
          </div>
          <div className="statistics-combination">
            <span className="combination-name">👥 Two Pair</span>
            <span className="combination-count">{statistics.twoPair}</span>
          </div>
          <div className="statistics-combination">
            <span className="combination-name">👤 One Pair</span>
            <span className="combination-count">{statistics.onePair}</span>
          </div>
          <div className="statistics-combination">
            <span className="combination-name">💔 Bust</span>
            <span className="combination-count">{statistics.bust}</span>
          </div>
        </div>
      </div>
    </div>
  );
}