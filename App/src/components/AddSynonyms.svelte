
<script lang="ts">
	import { maxSynonymLength, serviceUrl } from './Constants';
	import { sanitizeWord } from './Utils';

	let word = "";
	let synonyms = [];

	let currentWordText = "";
	let currentSynonymText = "";

	$: canPublish = synonyms.length > 0 && word.length > 0;
	let publishError = undefined;

	function deleteSynonym(idx: number) {
		synonyms = [...synonyms.slice(0, idx), ...synonyms.slice(idx + 1)]
	}

	function addSynonym() {
		const sanitizedText = sanitizeWord(currentSynonymText);
		if (sanitizedText.length > 0 && synonyms.indexOf(sanitizedText) === -1 && sanitizedText !== word) {
			synonyms = [...synonyms, sanitizedText];
			currentSynonymText = "";
		}
	}

	function setWord() {
		word = sanitizeWord(currentWordText);
		currentWordText = "";
	}

	function clearWord() {
		word = "";
	}

	async function publish() {
		console.log("Publish to remote: " + synonyms);
		try {
			const response = await fetch(`${serviceUrl}/api/synonyms?word=${word}`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify(synonyms)
			});

			if (response.ok) {
				word = "";
				synonyms = [];
				currentSynonymText = "";
				publishError = undefined;
			} else {
				const text = await response.text();
				console.error("Unexpected error when publishing: " + text);
				publishError = response.statusText + " (see the log for details)";
			}
		} catch (error) {
			console.error("Unexpected error when publishing: " + error.message);
			publishError = "Service unavailable (see the log for details)";
		}
	}
</script>

<div>
	<h1>Add synonyms</h1>
	{#if word.length === 0}
		<p>No word to publish yet...</p>

		<form on:submit|preventDefault={setWord}>
			<input type="text" placeholder="Set word" maxlength="{maxSynonymLength}" bind:value={currentWordText}>
		</form>
	{:else}
		<button class="synonym word" on:click={clearWord}>
			{word}

			<div class="synonym-overlay">
				<img src="/cross.svg" alt="Delete?" class="synonym-overlay-image">
			</div>
		</button>
	{/if}

	<div class="list-horizontal">
		{#each synonyms as synonym, idx}
			<button class="synonym" on:click={ e => deleteSynonym(idx) }>
				{synonym}

				<div class="synonym-overlay">
					<img src="/cross.svg" alt="Delete?" class="synonym-overlay-image">
				</div>
			</button>
		{/each}

		{#if !synonyms.length}
			<p>No synonyms to publish yet...</p>
		{/if}
	</div>

	<form on:submit|preventDefault={addSynonym}>
		<input type="text" placeholder="Add synonym" maxlength="{maxSynonymLength}" bind:value={currentSynonymText}>
	</form>

	<button on:click={publish} disabled={!canPublish}>
		Publish
	</button>

	{#if publishError}
		<p class="error-message">Failed to publish synonyms: <br/> {publishError}</p>
	{/if}
</div>

<style>
	.synonym-overlay {
		position: absolute;
		bottom: 0;
		left: 0;
		right: 0;
		box-sizing: border-box;
		width: 100%;
		height: 100%;
		overflow: hidden;

		background-color: rgb(138, 138, 138);
		border-radius: inherit;

		opacity: 0%;
		transition: .3s ease;
	}

	.synonym-overlay-image {
		position: absolute;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
	}

	.synonym-overlay:hover {
		opacity: 50%;
	}

	.word {
		color: rgb(14, 118, 255);
	}

	.list-horizontal {
		width: 50%;
		display: flex;
		flex-wrap: wrap;
		justify-content: center;
	}
</style>