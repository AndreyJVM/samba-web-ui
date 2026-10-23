package mari.samba;

import mari.samba.config.SambaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(SambaProperties.class)
public class SambryxApplication {
    public static void main(String[] args) {
        SpringApplication.run(SambryxApplication.class, args);
    }
}
