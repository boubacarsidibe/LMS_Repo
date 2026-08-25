package inpg.sn.lms.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String ressource, String champ, Object valeur) {
        super(String.format("%s introuvable avec %s = '%s'", ressource, champ, valeur));
    }
}