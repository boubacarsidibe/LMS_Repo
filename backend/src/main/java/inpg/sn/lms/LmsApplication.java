package inpg.sn.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling} : requis par {@code EvaluationRappelService}
 * (TDR §G, besoin B4) — première tâche planifiée de ce backend, voir
 * CLAUDE.md pour ce précédent.
 */
@SpringBootApplication
@EnableScheduling
public class LmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmsApplication.class, args);
	}

}
