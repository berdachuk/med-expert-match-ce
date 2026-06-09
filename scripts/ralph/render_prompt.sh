#!/usr/bin/env bash
# Render a single story into the agent prompt template.
#
# Usage: render_prompt.sh <STORY_ID>
#        STORY_ID format: M77-02
#
# Reads .agents/plans/{MILESTONE_ID}-stories.json, extracts the story,
# expands the .agents/templates/M{NN}-prompt.md.template via envsubst,
# and prints the rendered markdown on stdout.
#
# Exit codes:
#   0  Success
#   2  Missing argument / stories file / story id
#   5  Template file missing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ $# -lt 1 ]; then
    echo "Usage: $(basename "$0") <STORY_ID>" >&2
    exit 2
fi

STORY_ID="$1"
MILESTONE_ID="${STORY_ID%%-*}"
STORIES_FILE="$REPO_ROOT/.agents/plans/${MILESTONE_ID}-stories.json"
TEMPLATE_FILE="$REPO_ROOT/.agents/templates/M{NN}-prompt.md.template"

if [ ! -f "$STORIES_FILE" ]; then
    echo "ERROR: $STORIES_FILE not found" >&2
    exit 2
fi

if [ ! -f "$TEMPLATE_FILE" ]; then
    echo "ERROR: $TEMPLATE_FILE not found" >&2
    exit 5
fi

# Pull the story fields
STORY_JSON=$(jq -c --arg id "$STORY_ID" '.stories[] | select(.id == $id)' "$STORIES_FILE")
if [ -z "$STORY_JSON" ] || [ "$STORY_JSON" = "null" ]; then
    echo "ERROR: story $STORY_ID not found in $STORIES_FILE" >&2
    exit 2
fi

STORY_TITLE=$(echo "$STORY_JSON" | jq -r '.title')
TEST_TARGET=$(echo "$STORY_JSON" | jq -r '.test_target // ""')
PHASE_REF=$(echo "$STORY_JSON" | jq -r '.phase_ref // ""')
ACCEPT_BLOCK=$(echo "$STORY_JSON" | jq -r '.accept[]' | sed 's/^/- /')
SKILLS_BLOCK=$(echo "$STORY_JSON" | jq -r '.skills_to_load[]' | sed 's/^/- .agents\/skills\//; s/$/\/SKILL.md/')
FILES_BLOCK=$(echo "$STORY_JSON" | jq -r '.files_touched[]?' | sed 's/^/- /')

# Render with envsubst (only expand $MILESTONE_ID, $STORY_ID, $STORY_TITLE, etc.)
export MILESTONE_ID STORY_ID STORY_TITLE TEST_TARGET PHASE_REF ACCEPT_BLOCK SKILLS_BLOCK FILES_BLOCK STORIES_FILE REPO_ROOT
envsubst < "$TEMPLATE_FILE"
