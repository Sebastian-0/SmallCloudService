package cloudservice;

import cloudservice.Database.Synonyms;
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
		assertEquals(ImmutableList.of(), database.getSynonyms("non existing", 10).synonyms);
	}

	@Test
	void addSingle() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		assertEquals(ImmutableList.of("b"), database.getSynonyms("a", 10).synonyms);
	}

	@Test
	void addDuplicate() {
		database.addSynonyms("a", ImmutableSet.of("b", "b"));
		assertEquals(ImmutableList.of("b"), database.getSynonyms("a", 10).synonyms);
	}

	@Test
	void addMultiple() {
		database.addSynonyms("a", ImmutableSet.of("b", "c", "d"));
		assertEquals(ImmutableList.of("b", "c", "d"), database.getSynonyms("a", 10).synonyms);
	}

	@Test
	void addMultipleTimes() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		database.addSynonyms("a", ImmutableSet.of("c", "d"));
		assertEquals(ImmutableList.of("b", "c", "d"), database.getSynonyms("a", 10).synonyms);
	}

	@Test
	void addDifferent() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		database.addSynonyms("x", ImmutableSet.of("y", "z"));
		assertEquals(ImmutableList.of("b"), database.getSynonyms("a", 10).synonyms);
		assertEquals(ImmutableList.of("y", "z"), database.getSynonyms("x", 10).synonyms);
	}

	@Test
	void bidirectionality() {
		database.addSynonyms("a", ImmutableSet.of("b", "c"));
		assertEquals(ImmutableList.of("b", "c"), database.getSynonyms("a", 10).synonyms);
		assertEquals(ImmutableList.of("a", "c"), database.getSynonyms("b", 10).synonyms);
		assertEquals(ImmutableList.of("a", "b"), database.getSynonyms("c", 10).synonyms);
	}

	@Test
	void transitivity() {
		database.addSynonyms("a", ImmutableSet.of("b"));
		database.addSynonyms("b", ImmutableSet.of("c"));
		assertEquals(ImmutableList.of("b", "c"), database.getSynonyms("a", 10).synonyms);
		assertEquals(ImmutableList.of("a", "b"), database.getSynonyms("c", 10).synonyms);
	}

	@Test
	void sortOrder() {
		database.addSynonyms("a", ImmutableSet.of("x", "D", "d", "c", "e", "1"));
		assertEquals(ImmutableList.of("1", "c", "D", "d", "e", "x"), database.getSynonyms("a", 10).synonyms);
	}

	@Test
	void limit() {
		database.addSynonyms("a", ImmutableSet.of("b", "c", "d"));
		assertEquals(ImmutableList.of("b", "c", "d"), database.getSynonyms("a", 10).synonyms);
		assertEquals(ImmutableList.of("b", "c", "d"), database.getSynonyms("a", 3).synonyms);
		assertEquals(ImmutableList.of("b", "c"), database.getSynonyms("a", 2).synonyms);
		assertEquals(ImmutableList.of("b"), database.getSynonyms("a", 1).synonyms);
		assertEquals(ImmutableList.of(), database.getSynonyms("a", 0).synonyms);
	}

	@Test
	void returnedCounts() {
		database.addSynonyms("a", ImmutableSet.of("b", "c", "d"));

		Synonyms actual = database.getSynonyms("a", 10);
		assertEquals(3, actual.total);
		assertEquals(ImmutableList.of("b", "c", "d"), actual.synonyms);

		actual = database.getSynonyms("a", 2);
		assertEquals(3, actual.total);
		assertEquals(ImmutableList.of("b", "c"), actual.synonyms);

		actual = database.getSynonyms("a", 0);
		assertEquals(3, actual.total);
		assertEquals(ImmutableList.of(), actual.synonyms);
	}

	@Test
	void returnedCountsUnknownWord() {
		Synonyms actual = database.getSynonyms("x", 10);
		assertEquals(0, actual.total);
		assertEquals(ImmutableList.of(), actual.synonyms);
	}
}
