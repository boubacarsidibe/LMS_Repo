package inpg.sn.lms.recrutement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Création / mise à jour d'un programme de formation.
 *
 * <p>Ce DTO remplace le {@code @RequestBody Program} d'origine, qui liait
 * directement l'entité JPA à la charge utile. Deux conséquences, dont la
 * seconde n'était pas volontaire :
 *
 * <ul>
 *   <li>{@code title} et {@code level} sont {@code nullable = false} en base :
 *       une charge utile sans eux ne provoquait pas un 400 mais une violation
 *       de contrainte, donc un 500 illisible pour l'appelant ;</li>
 *   <li>{@code Program.sessions} est en {@code cascade = MERGE, PERSIST} : une
 *       charge utile contenant un tableau {@code sessions} pouvait ainsi créer
 *       ou modifier des sessions de candidature au passage, par un endpoint
 *       censé ne toucher qu'un libellé de programme.</li>
 * </ul>
 */
@Getter
@Setter
public class ProgramRequest {

    @NotBlank(message = "L'intitulé du programme est obligatoire.")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Le niveau du programme est obligatoire.")
    @Size(max = 255)
    private String level;

    @Size(max = 255)
    private String department;
}
