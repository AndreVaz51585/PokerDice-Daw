import React, { useEffect, useReducer } from "react";
import { useParams } from "react-router";
import { api, ApiError } from "../api";
import { Wallet } from "../types";
import { useAuth } from "../AuthContext";
import "../styles/App.css";

// State type
type State = {
  wallet: Wallet | null;
  isLoading: boolean;
  error: string | null;
  depositAmount: string;
  withdrawAmount: string;
  isDepositing: boolean;
  isWithdrawing: boolean;
  actionError: string | null;
};

// Action types
type Action =
  | { type: "load" }
  | { type: "set-wallet"; wallet: Wallet }
  | { type: "error"; error: string }
  | { type: "set-deposit-amount"; amount: string }
  | { type: "set-withdraw-amount"; amount: string }
  | { type: "depositing" }
  | { type: "withdrawing" }
  | { type: "action-complete"; newBalance: number }
  | { type: "action-error"; error: string };

// Reducer function
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "load":
      return { ...state, isLoading: true, error: null };
    case "set-wallet":
      return { ...state, wallet: action.wallet, isLoading: false, error: null };
    case "error":
      return { ...state, isLoading: false, error: action.error };
    case "set-deposit-amount":
      return { ...state, depositAmount: action.amount };
    case "set-withdraw-amount":
      return { ...state, withdrawAmount: action.amount };
    case "depositing":
      return { ...state, isDepositing: true, actionError: null };
    case "withdrawing":
      return { ...state, isWithdrawing: true, actionError: null };
    case "action-complete":
      return {
        ...state,
        isDepositing: false,
        isWithdrawing: false,
        actionError: null,
        depositAmount: "",
        withdrawAmount: "",
        wallet: state.wallet
          ? { ...state.wallet, currentBalance: action.newBalance }
          : null,
      };
    case "action-error":
      return {
        ...state,
        isDepositing: false,
        isWithdrawing: false,
        actionError: action.error,
      };
    default:
      return state;
  }
}

// Initial state
const initialState: State = {
  wallet: null,
  isLoading: true,
  error: null,
  depositAmount: "",
  withdrawAmount: "",
  isDepositing: false,
  isWithdrawing: false,
  actionError: null,
};

export function WalletView() {
  const { userId } = useParams<{ userId: string }>();
  const [state, dispatch] = useReducer(reducer, initialState);
  const { user } = useAuth();

  // Check if user can view this wallet
  const canView = user && userId && user.id === parseInt(userId);

  // Load wallet data
  useEffect(() => {
    if (!userId || !canView) return;

    async function loadWallet() {
      dispatch({ type: "load" });
      try {
        const wallet = await api.getWallet(Number(userId));
        dispatch({ type: "set-wallet", wallet });
      } catch (err) {
        if (err instanceof ApiError) {
          dispatch({ type: "error", error: err.message });
        } else {
          dispatch({ type: "error", error: "Erro ao carregar a carteira" });
        }
      }
    }

    loadWallet();
  }, [userId, canView]);

  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userId) return;

    const amount = parseFloat(state.depositAmount);
    if (isNaN(amount) || amount <= 0) {
      dispatch({ type: "action-error", error: "Montante inválido" });
      return;
    }

    dispatch({ type: "depositing" });
    try {
      const depositSucess = await api.deposit(Number(userId), amount);
      dispatch({ type: "action-complete", newBalance: depositSucess.currentBalance });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "action-error", error: err.message });
      } else {
        dispatch({ type: "action-error", error: "Erro ao depositar" });
      }
    }
  };

  const handleWithdraw = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userId) return;

    const amount = parseFloat(state.withdrawAmount);
    if (isNaN(amount) || amount <= 0) {
      dispatch({ type: "action-error", error: "Montante inválido" });
      return;
    }

    dispatch({ type: "withdrawing" });
    try {
      const withdrawSucess = await api.withdraw(Number(userId), amount);
      dispatch({ type: "action-complete", newBalance : withdrawSucess.currentBalance });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "action-error", error: err.message });
      } else {
        dispatch({ type: "action-error", error: "Erro ao levantar" });
      }
    }
  };

  if (!canView) {
    return (
      <div className="wallet-error">
        You do not have permission to view this wallet.
      </div>
    );
  }

  if (state.isLoading) {
    return <div className="wallet-loading">Loading wallet...</div>;
  }

  if (state.error || !state.wallet) {
    return (
      <div className="wallet-error">{state.error || "Wallet not found"}</div>
    );
  }

  const { wallet } = state;

  return (
    <div className="wallet-container">
      <div className="wallet-card">
        <h2> My Wallet</h2>
        <div className="wallet-balance">
          <span className="wallet-balance-label">Current Balance:</span>
          <span className="wallet-balance-value">{wallet.currentBalance}€</span>
        </div>

        {state.actionError && (
          <div className="wallet-action-error">{state.actionError}</div>
        )}

        <div className="wallet-actions">
          <div className="wallet-action-section">
            <h3>Deposit</h3>
            <form onSubmit={handleDeposit} className="wallet-form">
              <input
                type="number"
                value={state.depositAmount}
                onChange={(e) =>
                  dispatch({ type: "set-deposit-amount", amount: e.target.value })
                }
                placeholder="Amount"
                min="1"
                step="0.01"
                className="wallet-input"
              />
              <button
                type="submit"
                disabled={state.isDepositing}
                className="wallet-deposit-btn"
              >
                {state.isDepositing ? "Depositing..." : "Deposit"}
              </button>
            </form>
          </div>

          <div className="wallet-action-section">
            <h3>Withdraw</h3>
            <form onSubmit={handleWithdraw} className="wallet-form">
              <input
                type="number"
                value={state.withdrawAmount}
                onChange={(e) =>
                  dispatch({ type: "set-withdraw-amount", amount: e.target.value })
                }
                placeholder="Amount"
                min="1"
                step="0.01"
                max={wallet.currentBalance}
                className="wallet-input"
              />
              <button
                type="submit"
                disabled={state.isWithdrawing}
                className="wallet-withdraw-btn"
              >
                {state.isWithdrawing ? "Withdrawing..." : "Withdraw"}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}