import React, { useReducer } from "react";
import { useNavigate } from "react-router";
import { api, ApiError } from "../api";
import "../styles/App.css";

// State type
type State = {
  name: string;
  email: string;
  password: string;
  invitationCode: string;
  error: string | undefined;
  stage: "editing" | "posting" | "succeed" | "failed";
};

// Action types
type Action =
  | {
      type: "input-change";
      name: string;
      email: string;
      password: string;
      invitationCode: string;
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
        email: action.email,
        password: action.password,
        invitationCode: action.invitationCode,
      };
    case "post":
      return {
        ...state,
        stage: "posting",
        error: undefined,
      };
    case "success":
      return {
        name: "",
        email: "",
        password: "",
        invitationCode: "",
        error: undefined,
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
  email: "",
  password: "",
  invitationCode: "",
  error: undefined,
  stage: "editing",
};

export function Register() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    dispatch({ type: "post" });

    try {
      await api.createUser({
        name: state.name,
        email: state.email,
        password: state.password,
        invitationCode: state.invitationCode || undefined,
      });
      dispatch({ type: "success" });
      navigate("/login");
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({
          type: "error",
          message: "Ocorreu um erro durante o registo",
        });
      }
    }
  };

  return (
    <div className="register-container">
      <div className="register-card">
        <h2>Registar</h2>
        <form onSubmit={handleSubmit}>
          <div className="register-form-group">
            <label className="register-form-label">
              Nome:
              <input
                type="text"
                value={state.name}
                onChange={(e) =>
                  dispatch({
                    type: "input-change",
                    name: e.target.value,
                    email: state.email,
                    password: state.password,
                    invitationCode: state.invitationCode,
                  })
                }
                required
                className="register-form-input"
              />
            </label>
          </div>
          <div className="register-form-group">
            <label className="register-form-label">
              Email:
              <input
                type="email"
                value={state.email}
                onChange={(e) =>
                  dispatch({
                    type: "input-change",
                    name: state.name,
                    email: e.target.value,
                    password: state.password,
                    invitationCode: state.invitationCode,
                  })
                }
                required
                className="register-form-input"
              />
            </label>
          </div>
          <div className="register-form-group">
            <label className="register-form-label">
              Password:
              <input
                type="password"
                value={state.password}
                onChange={(e) =>
                  dispatch({
                    type: "input-change",
                    name: state.name,
                    email: state.email,
                    password: e.target.value,
                    invitationCode: state.invitationCode,
                  })
                }
                required
                className="register-form-input"
              />
            </label>
          </div>
          <div className="register-form-group">
            <label className="register-form-label">
              Código de Convite:
              <input
                type="text"
                value={state.invitationCode}
                onChange={(e) =>
                  dispatch({
                    type: "input-change",
                    name: state.name,
                    email: state.email,
                    password: state.password,
                    invitationCode: e.target.value,
                  })
                }
                className="register-form-input"
              />
            </label>
          </div>
          {state.error && <div className="register-error">{state.error}</div>}
          <button
            type="submit"
            disabled={state.stage === "posting"}
            className="register-submit-btn"
          >
            {state.stage === "posting" ? "A registar..." : "Registar"}
          </button>
        </form>
      </div>
    </div>
  );
}