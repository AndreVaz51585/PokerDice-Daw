import { useAuth } from "../AuthContext";
import "../styles/App.css";
import { InvitationCard } from "./invitation";

export function UserProfile() {
  const { user } = useAuth();

  if (!user) {
    return <div>A carregar...</div>;
  }

  return (
    <div className="user-profile-container">
      <div className="user-profile-card">
        <h2>O Meu Perfil</h2>
        <div className="user-profile-info">
          <div className="user-profile-field">
            <strong>ID:</strong> {user.id}
          </div>
          <div className="user-profile-field">
            <strong>Nome:</strong> {user.name}
          </div>
          <div className="user-profile-field">
            <strong>Email:</strong> {user.email}
          </div>
        </div>
        <InvitationCard />
      </div>
    </div>
  );
}