package gestion.pac.gestionstagiairesbackend.entite;

public class ValidationResult {


    private final boolean valid;
    private final int keywordMatches;
    private final int contentLength;

    public ValidationResult(boolean valid, int keywordMatches, int contentLength) {
        this.valid = valid;
        this.keywordMatches = keywordMatches;
        this.contentLength = contentLength;
    }

    public boolean isValid() {
        return valid;
    }

    public int getKeywordMatches() {
        return keywordMatches;
    }

    public int getContentLength() {
        return contentLength;
    }
}