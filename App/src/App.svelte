<script lang="ts">
	import AddSynonyms from './components/AddSynonyms.svelte';
	import ViewSynonyms from './components/ViewSynonyms.svelte';
</script>

<main>
	<AddSynonyms/>

	<hr/>

	<ViewSynonyms/>
</main>

<footer>
	<div>Favicon made by <a href="https://www.flaticon.com/authors/smashicons" title="Smashicons">Smashicons</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
</footer>

<style>
	main {
		/* Use this to expand the main surface so that the footer is in the right place */
		flex: 1;
	}

	hr {
		text-align: center;
		width: 50%;
	}

	footer {
		font-size: smaller;
		color: gray;
	}

	a {
		color: gray;
	}
</style>