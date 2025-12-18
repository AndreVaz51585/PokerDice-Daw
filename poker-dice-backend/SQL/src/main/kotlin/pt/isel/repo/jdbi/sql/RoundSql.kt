package pt.isel.repo.jdbi.sql

object RoundSql {
    const val SELECT_ROUND_BY_ID = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, started_at, ended_at
        FROM round
        WHERE id = :id
    """

    const val SELECT_ALL_ROUNDS = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, started_at, ended_at
        FROM round
    """

    const val SELECT_ROUNDS_BY_MATCH = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, started_at, ended_at
        FROM round
        WHERE match_id = :matchId
        ORDER BY number
    """

    const val SELECT_CURRENT_ROUND_BY_MATCH = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, started_at, ended_at
        FROM round
        WHERE match_id = :matchId AND status = 'IN_PROGRESS'
        ORDER BY number DESC
        LIMIT 1
    """

    const val INSERT_ROUND = """
        INSERT INTO round (match_id, number, ante_coins, pot_coins, started_at)
        VALUES (:matchId, :number, :anteCoins, :potCoins, :startedAt)
        RETURNING id
    """

    const val UPDATE_ROUND = """
        UPDATE round
        SET status = :status::round_state, pot_coins = :potCoins, ended_at = :endedAt
        WHERE id = :id
    """
    const val SELECT_ROUND_WINNERS = """
    SELECT u.id, u.name
    FROM round_winners rw
    JOIN dbo.users u ON rw.user_id = u.id
    WHERE rw.round_id = :roundId;
    """

    const val UPDATE_ROUND_WINNERS = """
    INSERT INTO round_winners (round_id, user_id)
    VALUES (:roundId, :userId);
    """

    const val DELETE_ROUND = """
        DELETE FROM round WHERE id = :id
    """

    const val DELETE_ALL_ROUNDS = """
        TRUNCATE TABLE round RESTART IDENTITY CASCADE
    """

    const val START_ROUND = """
        UPDATE round
        SET started_at = :startedAt
        WHERE id = :id
    """

    const val COMPLETE_ROUND = """
        UPDATE round
        SET status = :status, ended_at = :endedAt
        WHERE id = :id
    """

    const val UPDATE_ROUND_STATE = """
        UPDATE round
        SET status = :status::round_state
        WHERE id = :id
    """

    const val ADD_TO_POT = """
        UPDATE round
        SET pot_coins = pot_coins + :amount
        WHERE id = :id
    """

    const val GET_POT_AMOUNT = """
        SELECT pot_coins FROM round WHERE id = :id
    """

    const val COUNT_PLAYERS_PLAYED = """
        SELECT COUNT(DISTINCT user_id) 
        FROM dice 
        WHERE round_id = :roundId
    """

    const val COUNT_TOTAL_PLAYERS = """
        SELECT COUNT(DISTINCT user_id) 
        FROM match_player
        WHERE match_id = (SELECT match_id FROM round WHERE id = :roundId)
    """

    const val SELECT_HANDS_BY_ROUND = """
        SELECT user_id, d1, d2, d3
        FROM dice
        WHERE round_id = :roundId
        AND roll_no = (
            SELECT MAX(roll_no) 
            FROM dice d2 
            WHERE d2.round_id = dice.round_id 
            AND d2.user_id = dice.user_id
        )
    """

    const val CHECK_PLAYER_PLAYED = """
        SELECT COUNT(*) 
        FROM dice 
        WHERE round_id = :roundId 
        AND user_id = :userId
    """
}
