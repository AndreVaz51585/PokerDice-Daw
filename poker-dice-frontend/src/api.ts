import {
    UserInput,
    UserCreateTokenInputModel,
    UserCreateTokenOutputModel,
    User,
    Lobby,
    LobbyInput,
    Match,
    Wallet,
    AmountPayload,
    Statistics,
    CommandInput,
    InvitationId, DepositSucess, WithdrawSucess,
} from "./types";
import { getErrorDescription } from "./errorDescriptions";

const API_BASE_URL = "/api";

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error = await response
      .json()
      .catch(() => ({ title: "Unknown error" }));
    const errorMessage = error.title
      ? getErrorDescription(error.title)
      : response.statusText;
    throw new ApiError(response.status, errorMessage);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  // Check if response has content
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return response.json();
  }

  return undefined as T;
}

export const api = {
  // ==================== USERS ====================

  async createUser(input: UserInput): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/users`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });

    if (!response.ok) {
      const error = await response
        .json()
        .catch(() => ({ title: "Unknown error" }));
      const errorMessage = error.title
        ? getErrorDescription(error.title)
        : response.statusText;
      throw new ApiError(response.status, errorMessage);
    }

    return response.headers.get("Location") || "";
  },

  async createToken(
    input: UserCreateTokenInputModel
  ): Promise<UserCreateTokenOutputModel> {
    return fetchApi<UserCreateTokenOutputModel>("/users/token", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async logout(): Promise<void> {
    return fetchApi<void>("/logout", {
      method: "POST",
      headers: getAuthHeaders(),
    });
  },

  async getMe(): Promise<User> {
    return fetchApi<User>("/me", {
      headers: getAuthHeaders(),
    });
  },

  async getAllUsers(): Promise<User[]> {
    return fetchApi<User[]>("/users");
  },

  async deleteUser(userId: number): Promise<void> {
    return fetchApi<void>(`/users/${userId}`, {
      method: "DELETE",
    });
  },

  // ==================== LOBBIES ====================

  async createLobby(input: LobbyInput): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/lobbies`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getAuthHeaders(),
      },
      body: JSON.stringify(input),
    });

    if (!response.ok) {
      const error = await response
        .json()
        .catch(() => ({ title: "Unknown error" }));
      const errorMessage = error.title
        ? getErrorDescription(error.title)
        : response.statusText;
      throw new ApiError(response.status, errorMessage);
    }

    return response.headers.get("Location") || "";
  },

  async getAllLobbies(): Promise<Lobby[]> {
    return fetchApi<Lobby[]>("/lobbies");
  },

  async getLobbyById(lobbyId: number): Promise<Lobby> {
    return fetchApi<Lobby>(`/lobbies/${lobbyId}`);
  },

  async joinLobby(lobbyId: number): Promise<{ matchId?: number }> {
    return fetchApi<{ matchId?: number }>(`/lobbies/${lobbyId}/join`, {
      method: "POST",
      headers: getAuthHeaders(),
    });
  },

  async leaveLobby(lobbyId: number): Promise<void> {
    return fetchApi<void>(`/lobbies/${lobbyId}/leave`, {
      method: "POST",
      headers: getAuthHeaders(),
    });
  },

  async getLobbyPlayers(lobbyId: number): Promise<User[]> {
    return fetchApi<User[]>(`/lobbies/${lobbyId}/players`);
  },

  async getLobbyHost(lobbyId: number): Promise<User> {
    return fetchApi<User>(`/lobbies/${lobbyId}/host`);
  },

  // ==================== MATCHES ====================

  async getMatchById(matchId: number): Promise<Match> {
    return fetchApi<Match>(`/matches/${matchId}`);
  },

  async getMatchPlayers(matchId: number): Promise<User[]> {
    return fetchApi<User[]>(`/matches/${matchId}/players`);
  },

  async startMatch(matchId: number): Promise<void> {
    return fetchApi<void>(`/matches/${matchId}/start`, {
      method: "POST",
    });
  },

  async endMatch(matchId: number): Promise<void> {
    return fetchApi<void>(`/matches/${matchId}/end`, {
      method: "POST",
    });
  },

  // ==================== SSE MATCH COMMANDS ====================

  async sendCommand(
    matchId: number,
    command: CommandInput
  ): Promise<unknown> {
    return fetchApi(`/matches/sse/${matchId}/events`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(command),
    });
  },

  // ==================== WALLETS ====================

  async getAllWallets(): Promise<Wallet[]> {
    return fetchApi<Wallet[]>("/wallets");
  },

  async getWallet(userId: number): Promise<Wallet> {
    return fetchApi<Wallet>(`/wallets/${userId}`, {
      headers: getAuthHeaders(),
    });
  },

  async deposit(userId: number, amount: number): Promise<DepositSucess> {
    const payload: AmountPayload = { amount };
    return fetchApi<DepositSucess>(`/wallets/${userId}/deposit`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });
  },

  async withdraw(userId: number, amount: number): Promise<WithdrawSucess> {
    const payload: AmountPayload = { amount };
    return fetchApi<WithdrawSucess>(`/wallets/${userId}/withdraw`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });
  },

  // ==================== STATISTICS ====================

  async getAllStatistics(): Promise<Statistics[]> {
    return fetchApi<Statistics[]>("/statistics");
  },

  async getStatistics(userId: number): Promise<Statistics> {
    return fetchApi<Statistics>(`/statistics/${userId}`);
  },

  // ==================== INVITATIONS ====================

  async createInvitation(): Promise<InvitationId> {
    return fetchApi<InvitationId>("/invitations", {
      method: "POST",
      headers: getAuthHeaders(),
    });
  },
};

export { ApiError };