
<script type="ts">
    import { maxSynonymLength, serviceUrl } from './Constants';
	import { sanitizeWord } from './Utils';

	let searchPhrase = "";
	let searchResult = [];
	
	let currentSearchPhraseText = "";

	let searchPromise = Promise.resolve();
	let searchError = undefined;

	async function doSearch() {
		if (searchPhrase.length) {
			console.log(`Search for ${searchPhrase}`)
			try {
				const response = await fetch(`${serviceUrl}/api/synonyms?word=${searchPhrase.toLowerCase()}`);
				if (response.ok) {
					searchError = undefined;
					searchResult = await response.json();
				} else {
					const text = await response.text();
					console.error("Unexpected error when searching: " + text);
					searchError = response.statusText + " (see the log for details)";
				}
			} catch (error) {
				console.error("Unexpected error when searching: " + error.message);
				searchError = "Service unavailable (see the log for details)";
			}
		} else {
			searchError = undefined;
			searchResult = []
		}
	}

	function search() {
		const sanitized = sanitizeWord(currentSearchPhraseText);
		if (sanitized !== searchPhrase) {
			searchPhrase = sanitized;
			searchResult = []
			searchPromise = doSearch();
		}
	}
</script>


<div>
	<h1>View synonyms</h1>

	<form on:submit|preventDefault={search}>
		<input type="text" placeholder="Enter a word" maxlength="{maxSynonymLength}" bind:value={currentSearchPhraseText}>
	</form>

	{#await searchPromise}
		<p>Searching...</p>
	{:then}
		{#if searchPhrase.length == 0}
			<p>Empty search result...</p>
		{:else if searchResult.length == 0}
			<p>No synonyms found for '{searchPhrase}'</p>
		{:else}
			<p>Synonyms for '{searchPhrase}':</p>
		{/if}
	{/await}
	
	<div class="search-result">
		{#each searchResult as synonym}
			<div class="synonym">
				{synonym}
			</div>
		{/each}
	</div>

	{#if searchError}
		<p class="error-message">Failed to publish synonyms: <br/> {searchError}</p>
	{/if}
</div>


<style>
	.search-result {
		width: 50%;
	}
</style>