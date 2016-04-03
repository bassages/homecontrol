package nl.wiegman.home.repository;

import nl.wiegman.home.model.MindergasnlSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;

@Transactional
public interface MindergasnlSettingsRepository extends JpaRepository<MindergasnlSettings, Long> {

}
