package pt.isel.repo.jdbi.sql

object MatchSql {
    const val INSERT_MATCH = """
            INSERT INTO match (
                lobby_id, total_rounds, ante, state, current_round_no, started_at, finished_at
            ) VALUES (
                :lobbyId, :totalRounds, :ante, CAST(:state AS match_state), :currentRoundNo, :startedAt, :finishedAt
            )
        """

    const val INSERT_PLAYER = """
            INSERT INTO match_player (
                match_id, user_id, seat_no, balance_at_start, active
            ) VALUES (
                :matchId, :userId, :seatNo, :balanceAtStart, :active
            )
            ON CONFLICT DO NOTHING
        """

    const val SELECT_MATCH = """
            SELECT id, lobby_id, total_rounds, ante, state, current_round_no, started_at, finished_at
            FROM match
            WHERE id = :id
        """

    const val SELECT_MATCHES_PAGED = """
            SELECT id
            FROM match
            ORDER BY started_at DESC, id
            LIMIT :limit OFFSET :offset
        """

    const val SELECT_PLAYERS = """
            SELECT user_id, seat_no, balance_at_start, active
            FROM match_player
            WHERE match_id = :matchId
            ORDER BY seat_no
        """

    const val UPDATE_STATE = """
            UPDATE match
            SET state = CAST(:state AS match_state),
                finished_at = :finishedAt
            WHERE id = :id
        """

    const val UPDATE_CURRENT_ROUND = """
            UPDATE match
            SET current_round_no = :roundNo
            WHERE id = :id
        """

    const val UPDATE_MATCH = """
            UPDATE match
            SET lobby_id = :lobbyId,
                total_rounds = :totalRounds,
                ante = :ante,
                state = CAST(:state AS match_state),
                current_round_no = :currentRoundNo,
                started_at = :startedAt,
                finished_at = :finishedAt
            WHERE id = :id
        """

    const val DELETE_MATCH = """
            DELETE FROM match
            WHERE id = :id
        """

    const val DELETE_PLAYERS_BY_MATCH = """
            DELETE FROM match_player
            WHERE match_id = :matchId
        """

    const val DELETE_PLAYER = """
            DELETE FROM match_player
            WHERE match_id = :matchId AND user_id = :userId
        """

    const val UPDATE_PLAYER_ACTIVE = """
            UPDATE match_player
            SET active = :active
            WHERE match_id = :matchId AND user_id = :userId
        """

    const val COUNT_EXISTS = """
            SELECT COUNT(*) FROM match WHERE id = :id
        """

    const val CLEAR_MATCH_PLAYERS = "DELETE FROM match_player"
    const val CLEAR_MATCHES = "DELETE FROM match"
}