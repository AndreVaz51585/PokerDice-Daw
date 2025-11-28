package pt.isel.repo.jdbi.sql

object MatchSql {
    const val INSERT_MATCH = """
            INSERT INTO match (
                lobby_id, total_rounds, ante, state, current_round_no, started_at, finished_at, max_players
            ) VALUES (
                :lobbyId, :totalRounds, :ante, CAST(:state AS match_state), :currentRoundNo, :startedAt, :finishedAt,:maxPlayers
            )
        """


    const val INSERT_PLAYER = """
            INSERT INTO match_player (
                match_id, user_id, balance_start, seat_no
            ) VALUES (
                :matchId, :userId, :balanceStart, :seatNo
            )
            ON CONFLICT DO NOTHING
        """

    const val SELECT_MATCH = """
            SELECT id, lobby_id, total_rounds, ante, state, current_round_no, started_at, finished_at, max_players
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
            SELECT match_id, user_id, seat_no, balance_start, balance_end, active, turn
            FROM match_player
            WHERE match_id = :matchId
            ORDER BY seat_no
        """

    const val SELECT_WHO_TURN = """
            SELECT user_id
            FROM match_player
            WHERE match_id = :matchId AND turn = true
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
                max_players = :maxPlayers
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

    const val UPDATE_TURN = """
            UPDATE match_player
            SET turn = :turn
            WHERE match_id = :matchId AND user_id = :userId
        """

    const val COUNT_EXISTS = """
            SELECT COUNT(*) FROM match WHERE id = :id
        """

    const val CLEAR_MATCH_PLAYERS = "TRUNCATE TABLE match_player RESTART IDENTITY CASCADE;  "
    const val CLEAR_MATCHES = "TRUNCATE TABLE match RESTART IDENTITY CASCADE;"

    const val SELECT_MAX_SEAT = "SELECT COALESCE(MAX(seat_no), 0) FROM match_player WHERE match_id = :matchId"
}