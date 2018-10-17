package nl.homeserver.energie.mindergasnl;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

@Transactional
interface MindergasnlSettingsRepository extends JpaRepository<MindergasnlSettings, Long> {

    Optional<MindergasnlSettings> findOneByIdIsNotNull();
}