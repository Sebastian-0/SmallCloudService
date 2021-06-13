<script lang="ts">
	import AddSynonyms from './components/AddSynonyms.svelte';
	import ViewSynonyms from './components/ViewSynonyms.svelte';
</script>

<AddSynonyms/>

<hr/>

<ViewSynonyms/>

<footer>
	<div>Favicon made by <a href="https://www.flaticon.com/authors/smashicons" title="Smashicons">Smashicons</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
</footer>

<style>
	hr {
		text-align: center;
		width: 50%;
	}

	footer {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		font-size: smaller;
		color: gray;
	}

	a {
		color: gray;
	}
</style>