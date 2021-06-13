
<script type="ts">
    import { maxSynonymLength, serviceUrl } from './Constants';

	let searchPhrase = "";
	let searchResult = [];
	let searchError = undefined;

	async function search() {
		if (searchPhrase.length) {
			try {
				const response = await fetch(`${serviceUrl}/api/synonyms?word=${searchPhrase}`);
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
 
	function searchPhraseChanged() {
		let promise = search();
		promise.then( v => {
			console.log(`Search result: ${v}`)
		});
		console.log(`Search for ${searchPhrase}`)
	}
</script>


<div>
	<h1>View synonyms</h1>

	<input type="text" placeholder="Enter a word" maxlength="{maxSynonymLength}" bind:value={searchPhrase} on:input={searchPhraseChanged}>

	{#if searchResult.length > 0}
		<p>Synonyms for {searchPhrase}:</p>
	{:else}
		<p>Empty search result...</p>
	{/if}
	
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