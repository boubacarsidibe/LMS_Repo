package inpg.sn.lms.fichier;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ValidationFichierTest {

    @Test
    @DisplayName("Accepte une pièce PDF dont la signature correspond")
    void accepte_un_pdf_avec_signature_valide() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "diplome.pdf", "application/pdf",
                "%PDF-1.7\ncontenu".getBytes(StandardCharsets.US_ASCII));

        assertThatCode(() -> ValidationFichier.exiger(fichier,
                ValidationFichier.EXTENSIONS_DOCUMENT_CANDIDAT,
                ValidationFichier.TYPES_DOCUMENT_CANDIDAT, "PDF ou image"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Refuse un contenu HTML renommé en PDF")
    void refuse_un_contenu_renomme() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "diplome.pdf", "application/pdf",
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> ValidationFichier.exiger(fichier,
                ValidationFichier.EXTENSIONS_DOCUMENT_CANDIDAT,
                ValidationFichier.TYPES_DOCUMENT_CANDIDAT, "PDF ou image"));
    }
}
