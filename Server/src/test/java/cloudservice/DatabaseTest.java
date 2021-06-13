package cloudservice;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseTest {
	private Database database;

	@BeforeEach
	void setUp() {
		database = new Database();
	}

	@Test
	void nonExisting() {
		assertEquals(ImmutableList.of(), database.getSynonyms("non existing"));
	}

	@Test
	void addSingle() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		assertEquals(ImmutableList.of("b"), database.getSynonyms("a"));
	}

	@Test
	void addDuplicate() {
		database.addSynonyms("a", ImmutableSet.of("b", "b"));
		assertEquals(ImmutableList.of("b"), database.getSynonyms("a"));
	}

	@Test
	void addMultiple() {
		database.addSynonyms("a", ImmutableSet.of("b", "c", "d"));
		assertEquals(ImmutableList.of("b", "c", "d"), database.getSynonyms("a"));
	}

	@Test
	void addMultipleTimes() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		database.addSynonyms("a", ImmutableSet.of("c", "d"));
		assertEquals(ImmutableList.of("b", "c", "d"), database.getSynonyms("a"));
	}

	@Test
	void addDifferent() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		database.addSynonyms("x", ImmutableSet.of("y", "z"));
		assertEquals(ImmutableList.of("b"), database.getSynonyms("a"));
		assertEquals(ImmutableList.of("y", "z"), database.getSynonyms("x"));
	}

	@Test
	void bidirectionality() {
		database.addSynonyms("a", ImmutableSet.of("b", "c"));
		assertEquals(ImmutableList.of("b", "c"), database.getSynonyms("a"));
		assertEquals(ImmutableList.of("a", "c"), database.getSynonyms("b"));
		assertEquals(ImmutableList.of("a", "b"), database.getSynonyms("c"));
	}

	@Test
	void transitivity() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		database.addSynonyms("b", ImmutableSet.of("c"));
		assertEquals(ImmutableList.of("b", "c"), database.getSynonyms("a"));
		assertEquals(ImmutableList.of("a", "b"), database.getSynonyms("c"));
	}

	@Test
	void sortOrder() {
		database.addSynonyms("a", ImmutableSet.of("x", "D", "d", "c", "e", "1"));
		assertEquals(ImmutableList.of("1", "c", "D", "d", "e", "x"), database.getSynonyms("a"));
	}
}
