import { useState } from "react";
import { api, ApiError, getAuthHeaders } from "../api";
import "../styles/App.css";

export function InvitationCard() {
  const [isCreating, setIsCreating] = useState(false);
  const [invitationId, setInvitationId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleCreate = async () => {
  setIsCreating(true);
  setError(null);
  setInvitationId(null);

  try {
    const res = await api.createInvitation();

    if (res && (res as any).id) {
      setInvitationId(String((res as any).id));
      setError(null);
      return;
    }

    // fallback: POST directo e ler body como texto (apenas se necessário)
    try {
      const r = await fetch("/api/invitations", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(getAuthHeaders ? getAuthHeaders() : {}) },
      });
      if (r.ok) {
        const text = await r.text().catch(() => "");
        const cleaned = (text || "").trim().replace(/^"|"$/g, "");
        if (cleaned.length > 0) {
          setInvitationId(cleaned);
          setError(null);
          return;
        }
      }
      setError("Resposta inesperada do servidor ao criar o convite.");
    } catch {
      setError("Erro ao criar invitation (fallback falhou).");
    }
  } catch (err: any) {
    if (err instanceof ApiError) {
      setError(err.message ? `${err.message} (status ${err.status})` : `Erro (status ${err.status})`);
    } else {
      setError(err?.message ?? "Erro ao criar invitation");
    }
  } finally {
    setIsCreating(false);
  }
};


  return (
    <div className="user-profile-field invitation-inline">
      <div className="invitation-row">
        <button
          type="button"
          className="btn small"
          onClick={handleCreate}
          disabled={isCreating}
          aria-label="Generate invitation code"
          title="Generate invitation code"
        >
          {isCreating ? "Creating..." : "Invitation Code"}
        </button>

        {invitationId && (
          <>
            <input
              type="text"
              readOnly
              value={invitationId}
              aria-label="Invitation code"
              className="invitation-code-input"
            />
            <button
              type="button"
              onClick={() => navigator.clipboard.writeText(invitationId)}
              className="btn small"
              aria-label="Copy code"
            >
              Copy
            </button>
          </>
        )}
      </div>

      {!invitationId && error && (
        <div className="user-profile-error">{error}</div>
      )}
    </div>
  );
}

export default InvitationCard;