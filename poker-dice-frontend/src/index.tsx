import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router";
import { AuthProvider } from "./AuthContext";
 import { Layout } from "./components/layout";
import { Login } from "./components/login";

/*
import { Register } from "./components/Register";
import { UserProfile } from "./components/UserProfile";
import { LobbiesList } from "./components/LobbiesList";
import { LobbyDetails } from "./components/LobbyDetails";
import { CreateLobby } from "./components/CreateLobby";
import { MatchView } from "./components/MatchView";
import { WalletView } from "./components/WalletView";
import { StatisticsView } from "./components/StatisticsView";
import { ProtectedRoute } from "./components/ProtectedRoute";
*/

const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
      {
        path: "login",
        element: <Login />,
      },
      /*
      {
        index: true,
        element: <LobbiesList />,
      },

      {
        path: "register",
        element: <Register />,
      },
      {
        path: "me",
        element: (
          <ProtectedRoute>
            <UserProfile />
          </ProtectedRoute>
        ),
      },
      {
        path: "create-lobby",
        element: (
          <ProtectedRoute>
            <CreateLobby />
          </ProtectedRoute>
        ),
      },
      {
        path: "lobbies/:lobbyId",
        element: <LobbyDetails />,
      },
      {
        path: "matches/:matchId",
        element: (
          <ProtectedRoute>
            <MatchView />
          </ProtectedRoute>
        ),
      },
      {
        path: "wallet/:userId",
        element: (
          <ProtectedRoute>
            <WalletView />
          </ProtectedRoute>
        ),
      },
      {
        path: "statistics/:userId",
        element: <StatisticsView />,
      },
        */
    ],
  },
]);

createRoot(document.getElementById("container")!).render(
  <AuthProvider>
    <RouterProvider router={router} />
  </AuthProvider>
);