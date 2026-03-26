package games.jeopardy.server.repository;

import games.jeopardy.server.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link Player} accounts.
 *
 * <p>Extends {@link JpaRepository} to inherit standard CRUD operations
 * ({@code save}, {@code findById}, {@code findAll}, {@code delete}, etc.).
 * All custom queries below use derived method names so Spring Data JPA
 * generates the implementation automatically — no SQL or JPQL required.
 *
 * <p><b>Uniqueness checks:</b> {@code existsByUsername} and {@code existsByEmail}
 * are intentionally separate from the {@code findBy…} methods. Use them in
 * registration and profile-update flows to return a fast boolean without
 * fetching and hydrating a full {@link Player} object.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    /**
     * Looks up a player by their login handle.
     * Used during authentication — pair with a bcrypt check on
     * {@link Player#getPasswordHash()} in the auth service.
     *
     * @param username the exact username to match (case-sensitive per DB collation)
     * @return the matching player, or {@link Optional#empty()} if not found
     */
    Optional<Player> findByUsername(String username);

    /**
     * Looks up a player by email address.
     * Used for password-reset flows and to detect duplicate registrations.
     *
     * @param email the email address to match
     * @return the matching player, or {@link Optional#empty()} if not found
     */
    Optional<Player> findByEmail(String email);

    /**
     * Returns {@code true} if the username is already taken.
     * Call this before persisting a new {@link Player} to surface a
     * friendly validation error rather than catching a constraint violation.
     *
     * @param username the username to check
     */
    boolean existsByUsername(String username);

    /**
     * Returns {@code true} if the email address is already registered.
     * Call this before persisting a new {@link Player} to surface a
     * friendly validation error rather than catching a constraint violation.
     *
     * @param email the email address to check
     */
    boolean existsByEmail(String email);
}