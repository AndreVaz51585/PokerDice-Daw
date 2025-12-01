// Error descriptions mapping from backend Problem responses
export const errorDescriptions: Record<string, string> = {
  // User errors
  "email-already-in-use":
    "Já existe um utilizador com este endereço de email.",
  "insecure-password": "A password deve ter mais de 4 caracteres.",
  "user-not-found": "Utilizador não encontrado.",
  "user-or-password-are-invalid":
    "O email ou password fornecidos são inválidos. Por favor verifique as suas credenciais e tente novamente.",
  "invitation-code-required":
    "É necessário um código de convite para se registar.",
  "invalid-invitation-code":
    "O código de convite fornecido é inválido.",
  "invitation-code-already-used":
    "Este código de convite já foi utilizado.",
  "error-creating-user":
    "Ocorreu um erro ao criar o utilizador.",
  "error-deleting-user":
    "Ocorreu um erro ao eliminar o utilizador.",

  // Lobby errors
  "lobby-not-found": "Lobby não encontrado.",
  "lobby-closed": "Este lobby está fechado.",
  "lobby-full": "Este lobby está cheio.",
  "already-in-lobby": "Já está neste lobby.",
  "error-joining-lobby": "Erro ao entrar no lobby.",
  "error-leaving-lobby": "Erro ao sair do lobby.",
  "error-creating-lobby": "Erro ao criar o lobby.",
  "user-is-not-in-lobby": "Não está neste lobby.",
  "not-enough-money": "Não tem dinheiro suficiente para esta ação.",
  "user-already-in-another-lobby": "Já está noutro lobby.",

  // Match errors
  "match-not-found": "Partida não encontrada.",
  "already-in-match": "Já está numa partida.",
  "match-full": "Esta partida está cheia.",
  "invalid-request": "Pedido inválido.",
  "not-your-turn": "Não é a sua vez de jogar.",
  "command-unknown": "Comando desconhecido.",
  "invalid-indices": "Índices inválidos.",
  "invalid-body-parameters": "Parâmetros do corpo do pedido inválidos.",

  // Wallet errors
  "wallet-not-found": "Carteira não encontrada.",
  "no-permission": "Não tem permissão para esta ação.",
  "invalid-amount": "Montante inválido.",

  // Statistics errors
  "statistics-not-found": "Estatísticas não encontradas.",

  // Generic errors
  "unknown": "Ocorreu um erro desconhecido.",
};

export function getErrorDescription(errorType: string): string {
  return errorDescriptions[errorType] || errorType;
}