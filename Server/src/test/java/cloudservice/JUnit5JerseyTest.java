package cloudservice;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class JUnit5JerseyTest extends JerseyTest {
	@BeforeEach
	void before() throws Exception {
		super.setUp();
	}

	@AfterEach
	void after() throws Exception {
		super.tearDown();
	}
}
