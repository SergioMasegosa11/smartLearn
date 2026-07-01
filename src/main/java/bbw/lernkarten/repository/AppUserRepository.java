package bbw.lernkarten.repository;

import java.util.Optional;

import bbw.lernkarten.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}

