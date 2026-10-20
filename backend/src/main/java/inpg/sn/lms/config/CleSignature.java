package inpg.sn.lms.config;

/**
 * Clé HMAC qui scelle les certificats et les attestations d'accréditation.
 *
 * <p>{@code application.properties} annonce un repli sur {@code JWT_SECRET} quand
 * {@code CERTIFICAT_SIGNATURE_SECRET} n'est pas définie. Or Spring n'applique ce
 * repli que si la variable est <strong>absente</strong> : une variable définie mais
 * <strong>vide</strong> — c'est exactement ce que produit {@code cp .env.example .env},
 * qui liste {@code CERTIFICAT_SIGNATURE_SECRET=} sans valeur — passe telle quelle, et
 * {@code SecretKeySpec} refuse alors une clé vide. L'application démarrait, puis
 * plantait dans le {@code CommandLineRunner} de données de démonstration
 * (« Empty key »), sans que rien n'indique la variable en cause.
 *
 * <p>Ici, une valeur vide ou blanche vaut « non définie » : on retombe sur le secret
 * JWT, et on refuse de démarrer, avec un message qui nomme les variables, si aucune
 * des deux n'existe.
 */
public final class CleSignature {

    private CleSignature() {
    }

    public static String effective(String cleDediee, String secretJwt) {
        if (cleDediee != null && !cleDediee.isBlank()) {
            return cleDediee;
        }
        if (secretJwt != null && !secretJwt.isBlank()) {
            return secretJwt;
        }
        throw new IllegalStateException(
                "Aucune clé de signature des certificats : renseignez CERTIFICAT_SIGNATURE_SECRET "
                        + "(ou, à défaut, JWT_SECRET) dans backend/.env.");
    }
}
