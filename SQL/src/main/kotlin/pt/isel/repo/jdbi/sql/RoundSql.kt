package pt.isel.repo.jdbi.sql

object RoundSql {
    const val SELECT_ROUND_BY_ID = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, winner_user_id, started_at, ended_at
        FROM round
        WHERE id = :id
    """

    const val SELECT_ALL_ROUNDS = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, winner_user_id, started_at, ended_at
        FROM round
    """

    const val SELECT_ROUNDS_BY_MATCH = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, winner_user_id, started_at, ended_at
        FROM round
        WHERE match_id = :match_id
        ORDER BY number
    """

    const val SELECT_CURRENT_ROUND_BY_MATCH = """
        SELECT id, match_id, number, ante_coins, status, pot_coins, winner_user_id, started_at, ended_at
        FROM round
        WHERE match_id = :match_id AND status = 'IN_PROGRESS'
        ORDER BY number DESC
        LIMIT 1
    """

    const val INSERT_ROUND = """
        INSERT INTO round (match_id, number, ante_coins, pot_coins, winner_user_id, started_at)
        VALUES (:match_id, :number, :ante_coins, :pot_coins, :winner_user_id, :started_at)
        RETURNING id
    """

    const val UPDATE_ROUND = """
        UPDATE round
        SET status = :status, pot_coins = :pot_coins, winner_user_id = :winner_user_id, ended_at = :ended_at
        WHERE id = :id
    """

    const val DELETE_ROUND = """
        DELETE FROM round WHERE id = :id
    """

    const val DELETE_ALL_ROUNDS = """
        DELETE FROM round
    """

    const val START_ROUND = """
        UPDATE round
        SET started_at = :started_at
        WHERE id = :id
    """

    const val COMPLETE_ROUND = """
        UPDATE round
        SET winner_user_id = :winner_user_id, status = :status, ended_at = :ended_at
        WHERE id = :id
    """

    const val UPDATE_ROUND_STATE = """
        UPDATE round
        SET status = :status
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
        WHERE round_id = :round_id
    """

    const val COUNT_TOTAL_PLAYERS = """
        SELECT COUNT(DISTINCT user_id) 
        FROM match_player
        WHERE match_id = (SELECT match_id FROM round WHERE id = :round_id)
    """

    const val SELECT_HANDS_BY_ROUND = """
        SELECT user_id, d1, d2, d3
        FROM dice
        WHERE round_id = :round_id
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
        WHERE round_id = :round_id 
        AND user_id = :user_id
    """
}
