// User types
export interface User {
  id: number;
  name: string;
  email: string;
}

export interface UserInput {
  name: string;
  email: string;
  password: string;
  invitationCode?: string;
}

export interface UserCreateTokenInputModel {
  email: string;
  password: string;
}

export interface UserCreateTokenOutputModel {
  token: string;
}
// ---------------------------------------------- WALLETS ---------------------------------------------- //
// Wallet types
export interface Wallet {
  id: number;
  userId: number;
  currentBalance: number;
}

export interface AmountPayload {
  amount: number;
}

export interface DepositSucess {
    deposited: number,
    currentBalance: number,
    message: String
}

export interface WithdrawSucess {
    amountWithdrawn: number,
    currentBalance: number,
    message: String
}




//---------------------------------------------- STATISTICS --------------------------------------------- //

// Statistics types
export interface Statistics {
  id: number;
  userId: number;
  gamesPlayed: number;
  gamesWon: number;
  fiveOfAKind: number;
  fourOfAKind: number;
  fullHouse: number;
  straight: number;
  threeOfAKind: number;
  twoPair: number;
  onePair: number;
  bust: number;
}

// Lobby types
export interface Lobby {
  id: number;
  name: string;
  description: string;
  hostUserId: number;
  minPlayers: number;
  maxPlayers: number;
  rounds: number;
  ante: number;
  state: LobbyState;
}

export type LobbyState = "OPEN" | "FULL" | "STARTED";

export interface LobbyInput {
  name: string;
  description: string;
  minPlayers: number;
  maxPlayers: number;
  rounds: number;
  ante: number;
}

// Match types
export interface Match {
  id: number;
  lobbyId: number;
  totalRounds: number;
  currentRound: number;
  ante: number;
  state: MatchState;
}

export type MatchState = "WAITING" | "RUNNING" | "FINISHED";

export interface MatchPlayer {
  id: number;
  matchId: number;
  userId: number;
}

export interface MatchInput {
  lobbyId: number;
  totalRounds: number;
  ante: number;
}

// Game types - Dice faces
export type Face = "NINE" | "TEN" | "JACK" | "QUEEN" | "KING" | "ACE";

export interface Dice {
  value: Face;
}

export interface Hand {
  dices: Dice[];
}

// Combination types
export type CombinationType =
  | "FIVE_OF_A_KIND"
  | "FOUR_OF_A_KIND"
  | "FULL_HOUSE"
  | "STRAIGHT"
  | "THREE_OF_A_KIND"
  | "TWO_PAIR"
  | "ONE_PAIR"
  | "BUST";

export interface Combination {
  type: CombinationType;
  rank: number;
}

// Round types
export type RoundState = "OPEN" | "SCORING" | "CLOSED";

export interface Round {
  id: number;
  matchId: number;
  roundNumber: number;
  state: RoundState;
}

// Game phase types
export type GamePhase = "LOBBY" | "ROLLING" | "FINISHED";

// SSE Event types
export interface MatchSnapshot {
  matchId: number;
  game: GameState;
  wallets: { [userId: number]: Wallet };
  currentPlayerId: number | null;
  eventType: string;
  eventId: string;
}

export interface LobbySnapshot {
  matchId: number;
  game: GameState;
  wallets: { [userId: number]: Wallet };
  currentPlayerId: number | null;
  eventType: string;
  eventId: string;
}


export interface GameState {
  phase: GamePhase;
  rounds: RoundState[];
  currentRoundIndex: number;
  ante: number;
  players: PlayerState[];
}

export interface PlayerState {
  userId: number;
  hand: Hand | null;
  combination: Combination | null;
  hasRolled: boolean;
  rollsRemaining: number;
  heldIndices: number[];
}

// Command types for SSE
export interface CommandInput {
  type: "ROLL" | "HOLD" | "FINISH_TURN" | "NEXT_ROUND";
  indices?: number[];
}

// Invitation types
export interface InvitationId {
  id: string;
}

// Problem type for errors
export interface Problem {
  type: string;
  title: string;
  status: number;
  detail?: string;
}