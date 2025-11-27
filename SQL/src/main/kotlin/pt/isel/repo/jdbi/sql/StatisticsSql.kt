package pt.isel.repo.jdbi.sql

object StatisticsSql {
    const val CREATE_STATISTICS = """
        INSERT INTO statistics (user_id) VALUES (:user_id);
    """

    const val DELETE_BY_ID = """
        DELETE FROM statistics WHERE user_id = :user_id
    """

    const val CLEAR_STATISTICS = """
        DELETE FROM statistics
    """

    const val SELECT_STATISTICS = """
        SELECT * 
        FROM statistics 
        WHERE user_id = :user_id
    """

    const val SELECT_ALL_STATISTICS = """
            SELECT *
            FROM statistics
        """

    const val INCREMENT_GAMES = """
        UPDATE statistics
        SET games_played = games_played + 1
        WHERE user_id = :userId;
     """

    const val INCREMENT_GAMES_WON = """
        UPDATE statistics
        SET games_won = games_won + 1
        WHERE user_id = :userId;
     """

    const val INCREMENT_FIVE_OF_A_KIND = """
        UPDATE statistics
        SET five_of_a_kind = five_of_a_kind + 1
        WHERE user_id = :userId;
    """

    const val INCREMENT_FOUR_OF_A_KIND = """
        UPDATE statistics
        SET four_of_a_kind = four_of_a_kind + 1
        WHERE user_id = :userId;
    """

    const val INCREMENT_FULL_HOUSE = """
        UPDATE statistics
        SET full_house = full_house + 1
        WHERE user_id = :userId;
    """

    const val INCREMENT_STRAIGHT = """
        UPDATE statistics
        SET straight = straight + 1
        WHERE user_id = :userId;
    """

    const val INCREMENT_THREE_OF_A_KIND = """
        UPDATE statistics
        SET three_of_a_kind = three_of_a_kind + 1
        WHERE user_id = :userId;
    """

    const val INCREMENT_TWO_PAIR = """
        UPDATE statistics
        SET two_pair = two_pair + 1
        WHERE user_id = :userId;
    """

    const val INCREMENT_ONE_PAIR = """
        UPDATE statistics
        SET one_pair = one_pair + 1
        WHERE user_id = :userId;
    """

    const val INCREMENT_BUST = """
        UPDATE statistics
        SET bust = bust + 1
        WHERE user_id = :userId;
    """

}