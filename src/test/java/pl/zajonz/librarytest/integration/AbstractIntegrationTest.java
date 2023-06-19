package pl.zajonz.librarytest.integration;

import org.junit.ClassRule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("dev")
public abstract class AbstractIntegrationTest {

    static Network network = Network.newNetwork();

    @ClassRule
    public static MySQLContainer<?> mySQLContainer = new MySQLContainer<>(
            DockerImageName.parse("mysql:latest"))
            .withNetwork(network)
            .withPassword("root")
            .withUsername("root")
            .withExposedPorts(3306)
            .withNetworkAliases("mysql");

    @ClassRule
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3-management"))
            .withNetwork(network)
            .withExposedPorts(5672, 15672)
            .withNetworkAliases("rabbitmq")
            .withAccessToHost(true);

    @ClassRule
    public static GenericContainer<?> loggingContainer = new GenericContainer(
            DockerImageName.parse("library-sys-logging"))
            .withNetwork(network)
            .withExposedPorts(8080)
            .withAccessToHost(true)
            .withNetworkAliases("logging")
            .dependsOn(rabbitMQContainer)
            .withFileSystemBind("/local/path/to/logs", "/container/path/to/logs", BindMode.READ_WRITE);

    @ClassRule
    public static GenericContainer<?> emailSenderContainer = new GenericContainer(
            DockerImageName.parse("library-sys-email-sender"))
            .withNetwork(network)
            .withExposedPorts(8080)
            .withNetworkAliases("email")
            .dependsOn(rabbitMQContainer)
            .withAccessToHost(true);

    static {
        rabbitMQContainer.start();
        mySQLContainer.start();
        emailSenderContainer.withEnv("rabbit-name", rabbitMQContainer.getAdminUsername());
        emailSenderContainer.withEnv("rabbit-pass", rabbitMQContainer.getAdminPassword());
        loggingContainer.withEnv("rabbit-name", rabbitMQContainer.getAdminUsername());
        loggingContainer.withEnv("rabbit-pass", rabbitMQContainer.getAdminPassword());
        loggingContainer.start();
        emailSenderContainer.start();
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitMQContainer.getFirstMappedPort());
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }


}
