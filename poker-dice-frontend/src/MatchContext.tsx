import { createContext, useContext, useState, ReactNode } from "react";

interface MatchContextType {
    currentMatchId: string | null;
    setCurrentMatch: (matchId: string | null) => void;
    clearCurrentMatch: () => void;
}

const MatchContext = createContext<MatchContextType | undefined>(undefined);

export function MatchProvider({ children }: { children: ReactNode }) {
    const [currentMatchId, setCurrentMatchId] = useState<string | null>(() => {
        // Recuperar do localStorage ao iniciar (persistência entre refreshes)
        return localStorage.getItem("currentMatchId");
    });

    const setCurrentMatch = (matchId: string | null) => {
        setCurrentMatchId(matchId);
        if (matchId) {
            localStorage.setItem("currentMatchId", matchId);
        } else {
            localStorage.removeItem("currentMatchId");
        }
    };

    const clearCurrentMatch = () => {
        setCurrentMatchId(null);
        localStorage.removeItem("currentMatchId");
    };

    return (
        <MatchContext.Provider value={{ currentMatchId, setCurrentMatch, clearCurrentMatch }}>
            {children}
        </MatchContext.Provider>
    );
}

export function useMatch() {
    const context = useContext(MatchContext);
    if (!context) {
        throw new Error("useMatch must be used within a MatchProvider");
    }
    return context;
}
