import React, { useReducer } from "react";
import { useNavigate } from "react-router";
import { api, ApiError } from "../api";
import "../styles/App.css";

// State type
type State = {
  name: string;
  description: string;
  minPlayers: number;
  maxPlayers: number;
  rounds: number;
  ante: number;
  error: string | undefined;
  stage: "editing" | "posting" | "succeed" | "failed";
};

// Action types
type Action =
  | {
      type: "input-change";
      name: string;
      description: string;
      minPlayers: number;
      maxPlayers: number;
      rounds: number;
      ante: number;
    }
  | { type: "post" }
  | { type: "success" }
  | { type: "error"; message: string };

// Reducer function
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "input-change":
      return {
        ...state,
        name: action.name,
        description: action.description,
        minPlayers: action.minPlayers,
        maxPlayers: action.maxPlayers,
        rounds: action.rounds,
        ante: action.ante,
      };
    case "post":
      return {
        ...state,
        stage: "posting",
        error: undefined,
      };
    case "success":
      return {
        ...initialState,
        stage: "succeed",
      };
    case "error":
      return {
        ...state,
        stage: "failed",
        error: action.message,
      };
    default:
      return state;
  }
}

// Initial state
const initialState: State = {
  name: "",
  description: "",
  minPlayers: 2,
  maxPlayers: 6,
  rounds: 5,
  ante: 10,
  error: undefined,
  stage: "editing",
};

export function CreateLobby() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    dispatch({ type: "post" });

    try {
      const location = await api.createLobby({
        name: state.name,
        description: state.description,
        minPlayers: state.minPlayers,
        maxPlayers: state.maxPlayers,
        rounds: state.rounds,
        ante: state.ante,
      });
      dispatch({ type: "success" });
      // Extract lobby ID from location header
      const lobbyId = location.split("/").pop();
      if (lobbyId) {
        navigate(`/lobbies/${lobbyId}`);
      } else {
        navigate("/");
      }
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({
          type: "error",
          message: "Ocorreu um erro ao criar o lobby",
        });
      }
    }
  };

  return (
    <div className="create-lobby-container">
      <div className="create-lobby-card">
        <h2>Criar Novo Lobby</h2>
        <form onSubmit={handleSubmit}>
          <div className="create-lobby-form-group">
            <label className="create-lobby-form-label">
              Nome do Lobby:
              <input
                type="text"
                value={state.name}
                onChange={(e) =>
                  dispatch({
                    type: "input-change",
                    name: e.target.value,
                    description: state.description,
                    minPlayers: state.minPlayers,
                    maxPlayers: state.maxPlayers,
                    rounds: state.rounds,
                    ante: state.ante,
                  })
                }
                required
                className="create-lobby-form-input"
                placeholder="Ex: Sala de Poker #1"
              />
            </label>
          </div>

          <div className="create-lobby-form-group">
            <label className="create-lobby-form-label">
              Descrição:
              <textarea
                value={state.description}
                onChange={(e) =>
                  dispatch({
                    type: "input-change",
                    name: state.name,
                    description: e.target.value,
                    minPlayers: state.minPlayers,
                    maxPlayers: state.maxPlayers,
                    rounds: state.rounds,
                    ante: state.ante,
                  })
                }
                className="create-lobby-form-textarea"
                placeholder="Descreva o seu lobby..."
              />
            </label>
          </div>

          <div className="create-lobby-form-row">
            <div className="create-lobby-form-group">
              <label className="create-lobby-form-label">
                Mínimo de Jogadores:
                <input
                  type="number"
                  value={state.minPlayers}
                  onChange={(e) =>
                    dispatch({
                      type: "input-change",
                      name: state.name,
                      description: state.description,
                      minPlayers: parseInt(e.target.value) || 2,
                      maxPlayers: state.maxPlayers,
                      rounds: state.rounds,
                      ante: state.ante,
                    })
                  }
                  min="2"
                  max="10"
                  required
                  className="create-lobby-form-input"
                />
              </label>
            </div>

            <div className="create-lobby-form-group">
              <label className="create-lobby-form-label">
                Máximo de Jogadores:
                <input
                  type="number"
                  value={state.maxPlayers}
                  onChange={(e) =>
                    dispatch({
                      type: "input-change",
                      name: state.name,
                      description: state.description,
                      minPlayers: state.minPlayers,
                      maxPlayers: parseInt(e.target.value) || 6,
                      rounds: state.rounds,
                      ante: state.ante,
                    })
                  }
                  min="2"
                  max="10"
                  required
                  className="create-lobby-form-input"
                />
              </label>
            </div>
          </div>

          <div className="create-lobby-form-row">
            <div className="create-lobby-form-group">
              <label className="create-lobby-form-label">
                Número de Rondas:
                <input
                  type="number"
                  value={state.rounds}
                  onChange={(e) =>
                    dispatch({
                      type: "input-change",
                      name: state.name,
                      description: state.description,
                      minPlayers: state.minPlayers,
                      maxPlayers: state.maxPlayers,
                      rounds: parseInt(e.target.value) || 5,
                      ante: state.ante,
                    })
                  }
                  min="1"
                  max="20"
                  required
                  className="create-lobby-form-input"
                />
              </label>
            </div>

            <div className="create-lobby-form-group">
              <label className="create-lobby-form-label">
                Ante (€):
                <input
                  type="number"
                  value={state.ante}
                  onChange={(e) =>
                    dispatch({
                      type: "input-change",
                      name: state.name,
                      description: state.description,
                      minPlayers: state.minPlayers,
                      maxPlayers: state.maxPlayers,
                      rounds: state.rounds,
                      ante: parseInt(e.target.value) || 10,
                    })
                  }
                  min="1"
                  required
                  className="create-lobby-form-input"
                />
              </label>
            </div>
          </div>

          {state.error && (
            <div className="create-lobby-error">{state.error}</div>
          )}

          <button
            type="submit"
            disabled={state.stage === "posting"}
            className="create-lobby-submit-btn"
          >
            {state.stage === "posting" ? "A criar..." : "Criar Lobby"}
          </button>
        </form>
      </div>
    </div>
  );
}