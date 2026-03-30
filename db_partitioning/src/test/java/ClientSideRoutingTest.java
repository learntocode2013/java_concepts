import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientSideRoutingTest {
    private static ClientSideRouting queryRouter;

    @BeforeAll
    static void setUp() {
        queryRouter = new ClientSideRouting();
    }

    @AfterAll
    static void tearDown() {
        queryRouter.shutDown();
    }

    @Test
    @Order(1)
    void canAddNewUsers() {
        queryRouter.insertUser(new User(1001, "alice_dev", "US")).get();
        queryRouter.insertUser(new User(1002, "bob_ops", "EU")).get();
        queryRouter.insertUser(new User(1003, "charlie_db", "APAC")).get();
        queryRouter.insertUser(new User(1004, "diana_sec", "US")).get();
    }

    @Test
    @Order(2)
    void canFetchUsers() {
        queryRouter.fetchUserById(1001).get();
        queryRouter.fetchUserById(1002).get();
        queryRouter.fetchUserById(1003).get();
        queryRouter.fetchUserById(1004).get();
    }
}