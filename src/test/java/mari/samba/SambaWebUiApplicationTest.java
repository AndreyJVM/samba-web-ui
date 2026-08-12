package mari.samba;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SambaWebUiApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("sambaWebUiApplication"));
    }

    @Test
    void testMainMethodSignature() {
        // Проверяем, что main метод существует и имеет правильную сигнатуру
        try {
            var method = SambaWebUiApplication.class.getMethod("main", String[].class);
            assertNotNull(method);
        } catch (NoSuchMethodException e) {
            fail("main method not found");
        }
    }

    @Test
    void testApplicationClassAnnotation() {
        assertTrue(SambaWebUiApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class
        ));
    }
}