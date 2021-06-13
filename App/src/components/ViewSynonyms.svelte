
<script type="ts">
    import { maxSynonymLength, serviceUrl } from './Constants';

	let searchPhrase = "";
	let searchError = undefined;
	let searchResult = search();


	async function search() : Promise<string[]> {
		if (searchPhrase.length) {
			try {
				const response = await fetch(`${serviceUrl}/api/synonyms?word=${searchPhrase}`);
				if (response.ok) {
					searchError = undefined;
					return response.json();
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
		}
		return Promise.resolve([]);
	}
 
	function searchPhraseChanged() {
		searchResult = search();
		searchResult.then( v => {
			console.log(`Search result: ${v}`)
		});
		console.log(`Search for ${searchPhrase}`)
	}
</script>


<div>
	<h1>View synonyms</h1>

	<input type="text" placeholder="Search for a synonym" maxlength="{maxSynonymLength}" bind:value={searchPhrase} on:input={searchPhraseChanged}>

	{#await searchResult}
		<div/> <!-- Empty div to make spacing correct before the result is loaded -->
	{:then result}
		<div class="search-result">
			{#each result as synonym}
				<div class="synonym">
					{synonym}
				</div>
			{/each}
		</div>
	{:catch error}
		<p class="error-message">There was an unexpected error: {error.message}</p>
	{/await}


    {#if searchError}
		<p class="error-message">Failed to publish synonyms: <br/> {searchError}</p>
    {/if}
</div>


<style>
	.search-result {
		width: 50%;
	}
</style>