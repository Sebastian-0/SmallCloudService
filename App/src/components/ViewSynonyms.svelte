
<script type="ts">
	let searchPhrase = "";
	let searchResult = search();

	async function search() : Promise<string[]> {
		if (searchPhrase.length) {
			// Send search request
			return new Promise((resolve, reject) => { resolve(["a", "b", "c"]); })
		}
		return new Promise((resolve, reject) => { resolve([]); });
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

	<input type="text" placeholder="Search for a synonym" bind:value={searchPhrase} on:input={searchPhraseChanged}>

	{#await searchResult then result}
		<div class="search-result">
			{#each result as synonym}
				<div class="synonym">
					{synonym}
				</div>
			{/each}
		</div>
	{:catch error}
		<p>There was an error: {error.message}</p>
	{/await}
</div>


<style>
	.search-result {
		width: 50%;
	}
</style>