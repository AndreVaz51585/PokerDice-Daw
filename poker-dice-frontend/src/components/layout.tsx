import { Link, Outlet, useNavigate } from "react-router";
import { useAuth } from "../AuthContext";
import "../styles/App.css";

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="layout-container">
      <nav className="layout-nav">
        <div className="layout-nav-left">
          <h1 className="layout-nav-title">🎲 Poker Dice</h1>
          <div className="layout-nav-links">
            <Link to="/" className="layout-nav-link">
              Lobbies
            </Link>
            {user && (
              <>
                <Link to="/create-lobby" className="layout-nav-link">
                  Criar Lobby
                </Link>
                <Link to={`/wallet/${user.id}`} className="layout-nav-link">
                  Carteira
                </Link>
                <Link to={`/statistics/${user.id}`} className="layout-nav-link">
                  Estatísticas
                </Link>
              </>
            )}
          </div>
        </div>
        <div className="layout-nav-right">
          {user ? (
            <>
              <Link to="/me" className="layout-nav-link">
                {user.name}
              </Link>
              <button onClick={handleLogout} className="layout-logout-btn">
                Sair
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="layout-nav-link">
                Entrar
              </Link>
              <Link to="/register" className="layout-nav-link">
                Registar
              </Link>
            </>
          )}
        </div>
      </nav>
      <main className="layout-main">
        <Outlet />
      </main>
    </div>
  );
}