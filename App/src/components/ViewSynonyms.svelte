
<script type="ts">
    import { maxSynonymLength, serviceUrl } from './Constants';
	import { sanitizeWord } from './Utils';

	class SynonymPage {
		total: number;
		synonyms: string[];

		constructor() {
			this.total = 0;
			this.synonyms = [];
		}
	}

	let pageLimit;
	let searchPhrase = "";
	let searchResult = new SynonymPage();
	
	let currentSearchPhraseText = "";

	let searchPromise = Promise.resolve();
	let searchError = undefined;

	async function doSearch() {
		if (searchPhrase.length) {
			console.log(`Search for ${searchPhrase}`)
			try {
				const response = await fetch(`${serviceUrl}/api/synonyms?word=${searchPhrase.toLowerCase()}&limit=${pageLimit}`);
				if (response.ok) {
					searchError = undefined;
					searchResult = await response.json();
				} else {
					const text = await response.text();
					console.error("Unexpected error when searching: " + text);
					searchError = response.statusText + " (see the log for details)";
					searchResult = new SynonymPage();
				}
			} catch (error) {
				console.error("Unexpected error when searching: " + error.message);
				searchError = "Service unavailable (see the log for details)";
				searchResult = new SynonymPage();
			}
		} else {
			searchError = undefined;
			searchResult = new SynonymPage();
		}
	}

	function searchPhraseChanged() {
		pageLimit = 100;
		searchPhrase = sanitizeWord(currentSearchPhraseText);
		searchPromise = doSearch();
	}

	function increaseLimit() {
		pageLimit += 100;
		searchPromise = doSearch();
	}
</script>


<div>
	<h1>View synonyms</h1>

	<input type="text" placeholder="Enter a word" maxlength="{maxSynonymLength}" bind:value={currentSearchPhraseText} on:input={searchPhraseChanged}>

	{#await searchPromise}
		<p>Searching...</p>
	{:then}
		{#if searchPhrase.length === 0}
			<p>Empty search result...</p>
		{:else if searchResult.total === 0}
			<p>No synonyms found for '{searchPhrase}'</p>
		{:else}
			<p>Synonyms for '{searchPhrase}':</p>
		{/if}
	{/await}
	
	<div class="search-result">
		{#each searchResult.synonyms as synonym}
			<div class="synonym">
				{synonym}
			</div>
		{/each}
	</div>

	<!-- It would be nice to use infinite scroll here but that will have to be another time -->
	{#if searchResult.total > searchResult.synonyms.length}
		<div>
			Showing first {searchResult.synonyms.length} of {searchResult.total}
			<button on:click="{increaseLimit}">Show more</button>
		</div>
	{/if}

	{#if searchError}
		<p class="error-message">Failed to search for synonyms: <br/> {searchError}</p>
	{/if}
</div>


<style>
	.search-result {
		width: 50%;
	}
</style>