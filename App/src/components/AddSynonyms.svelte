
<script lang="ts">
    import * as Constants from './Constants.ts';

    // let synonyms = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'x', 'y'];
    let synonyms = [];

    let currentSynonymText = "";
    $: canPublish = synonyms.length > 1;

    let publishError = undefined;

    function deleteSynonym(idx: number) {
        synonyms = [...synonyms.slice(0, idx), ...synonyms.slice(idx + 1)]
    }

    function addSynonym(event: Event) {
        const lowercaseText = currentSynonymText.toLowerCase();
        if (lowercaseText.length && synonyms.indexOf(lowercaseText) == -1) {
            synonyms = [...synonyms, lowercaseText];
            currentSynonymText = "";
        }
    }

    async function publish() {
        console.log("Publish to remote: " + synonyms);
        try {
            const response = await fetch(`http://localhost:8080/api/synonyms?word=${synonyms[0]}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(synonyms)
            });

            if (response.ok) {
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
    <div class="list-horizontal">
        {#each synonyms as synonym, idx}
            <button class="synonym" on:click={ event => deleteSynonym(idx) }>
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
        <input type="text" placeholder="Add synonym" maxlength="{Constants.maxSynonymLength}" bind:value={currentSynonymText}>
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

    .list-horizontal {
        width: 50%;
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
    }
</style>