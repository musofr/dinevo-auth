package it.dinevo.auth.services;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EstablishmentCodeValidator {

    private static final String ESTABLISHMENT_CODE_PREFIX = "establishment:code:";

    private final ValueCommands<String, String> commands;

    @Inject
    public EstablishmentCodeValidator(RedisDataSource dataSource) {
        this.commands = dataSource.value(String.class);
    }

    public record ValidationResult(Long establishmentId, String code) {}

    /**
     * Valida il codice establishment e restituisce l'establishmentId e il codice se valido
     *
     * @param code il codice monouso da validare
     * @return Uni con ValidationResult contenente establishmentId e code se valido, null se non valido/scaduto
     */
    public Uni<ValidationResult> validateAndConsumeCode(String code) {
        return Uni.createFrom().item(() -> {
            String key = ESTABLISHMENT_CODE_PREFIX + code;

            // Ottiene e rimuove il valore in un'unica operazione atomica
            String establishmentIdStr = commands.getdel(key);

            if (establishmentIdStr == null) {
                return null;
            }

            try {
                Long establishmentId = Long.parseLong(establishmentIdStr);
                return new ValidationResult(establishmentId, code);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    /**
     * Controlla se un codice esiste ed è valido (senza consumarlo)
     * Utile per testing o validazioni preliminari
     *
     * @param code il codice da controllare
     * @return true se il codice esiste ed è valido
     */
    public Uni<Boolean> isCodeValid(String code) {
        return Uni.createFrom().item(() -> {
            String key = ESTABLISHMENT_CODE_PREFIX + code;
            return commands.get(key) != null;
        });
    }
}
