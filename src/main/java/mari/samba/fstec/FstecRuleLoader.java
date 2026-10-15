package mari.samba.fstec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads FSTEC compliance rule definitions from YAML configuration.
 */
@Component
public class FstecRuleLoader {

    private List<FstecRuleDefinition> rules = List.of();

    @PostConstruct
    public void init() {
        loadRules();
    }

    public synchronized List<FstecRuleDefinition> loadRules() {
        if (!rules.isEmpty()) {
            return rules;
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = new ClassPathResource("fstec/rules-samba-2026.yml").getInputStream()) {
            List<FstecRuleDefinition> loaded = mapper.readValue(is, new TypeReference<List<FstecRuleDefinition>>() {});
            rules = Collections.unmodifiableList(loaded);
            return rules;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load FSTEC rules configuration from classpath", e);
        }
    }

    public List<FstecRuleDefinition> getRules() {
        if (rules.isEmpty()) {
            return loadRules();
        }
        return rules;
    }
}
