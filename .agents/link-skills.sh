#!/usr/bin/env bash
# Links this project's selected Agent Skills from the machine's shared skill library, so Claude Code and Codex load
# exactly the same skills from one physical copy.
#
#   .agents/link-skills.sh            create or repair the links (idempotent)
#   .agents/link-skills.sh --check    change nothing; exit 1 if any link is missing, stale or wrong
#
# The selection is .agents/skills.txt, one skill name per line (# starts a comment). Each name becomes the symlink
# .agents/skills/<name> -> $AGENT_SKILLS_HOME/<name> (default ~/.local/share/agent-skills). Claude Code reads the same
# directory through the tracked symlink .claude/skills -> ../.agents/skills. The links are machine-specific and
# git-ignored; nothing in CI needs them.
#
# Never overwritten: a real file or directory in .agents/skills (only symlinks are managed), a .claude/skills that is
# not the expected symlink, and a library skill whose SKILL.md is missing or names a different skill. Links to skills
# no longer selected are removed.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
selection="$repo_root/.agents/skills.txt"
skills_dir="$repo_root/.agents/skills"
claude_link="$repo_root/.claude/skills"
library="${AGENT_SKILLS_HOME:-$HOME/.local/share/agent-skills}"

check=false
case "${1:-}" in
  '') ;;
  --check) check=true ;;
  *) echo "Usage: $0 [--check]" >&2; exit 2 ;;
esac
(($# <= 1)) || { echo "Usage: $0 [--check]" >&2; exit 2; }

fail() { printf 'agent-skills: %s\n' "$*" >&2; exit 1; }
problems=0
report() { printf '  %s\n' "$*"; }
problem() { printf '  %s\n' "$*" >&2; problems=$((problems + 1)); }

[[ -f "$selection" ]] || fail "missing $selection"
[[ "$library" == /* ]] || fail "AGENT_SKILLS_HOME must be an absolute path"
[[ -d "$library" ]] || fail "shared skill library not found: $library"

# The selection: valid, unique names, each a complete skill in the library.
selected=()
declare -A wanted=()
while IFS= read -r line || [[ -n "$line" ]]; do
  name="${line%%#*}"
  name="${name//[[:space:]]/}"
  [[ -n "$name" ]] || continue
  [[ "$name" =~ ^[a-z0-9][a-z0-9-]*$ ]] || fail "invalid skill name in .agents/skills.txt: '$name'"
  [[ -z "${wanted[$name]:-}" ]] || fail "skill listed twice in .agents/skills.txt: $name"
  wanted[$name]=1
  selected+=("$name")
done < "$selection"
((${#selected[@]} > 0)) || fail ".agents/skills.txt selects no skill"

for name in "${selected[@]}"; do
  manifest="$library/$name/SKILL.md"
  [[ -f "$manifest" ]] || fail "$name: no SKILL.md in the shared library ($library/$name)"
  declared="$(sed -n '1{/^---[[:space:]]*$/!q};2,/^---[[:space:]]*$/{s/^name:[[:space:]]*//p}' "$manifest" | head -n 1)"
  declared="${declared%\"}"; declared="${declared#\"}"; declared="${declared%\'}"; declared="${declared#\'}"
  declared="${declared%"${declared##*[![:space:]]}"}"
  [[ "$declared" == "$name" ]] || fail "$name: the library's SKILL.md declares name '${declared:-<none>}'"
done

# .agents/skills must be a real directory: the old layout made it a symlink to a vendored copy.
if [[ -L "$skills_dir" ]]; then
  fail ".agents/skills is a symlink ($(readlink "$skills_dir")); check out the current branch, which tracks it as a directory"
fi
if [[ ! -d "$skills_dir" ]]; then
  if $check; then problem "missing directory .agents/skills"; else mkdir -p "$skills_dir"; report "created .agents/skills"; fi
fi

# Links for the selection.
for name in "${selected[@]}"; do
  link="$skills_dir/$name"
  target="$library/$name"
  if [[ -L "$link" ]]; then
    current="$(readlink "$link")"
    [[ "$current" == "$target" ]] && continue
    if $check; then problem "$name -> $current (expected $target)"; continue; fi
    ln -sfn "$target" "$link"
    report "relinked $name -> $target"
  elif [[ -e "$link" ]]; then
    problem "$name: .agents/skills/$name is a real file or directory, not a link; refusing to replace it"
  elif $check; then
    problem "missing link $name"
  else
    ln -s "$target" "$link"
    report "linked $name -> $target"
  fi
done

# Anything else in the directory: stale links go, real entries are reported and kept.
if [[ -d "$skills_dir" ]]; then
  for entry in "$skills_dir"/* "$skills_dir"/.[!.]*; do
    [[ -e "$entry" || -L "$entry" ]] || continue
    base="$(basename "$entry")"
    [[ "$base" == .gitignore ]] && continue
    [[ -n "${wanted[$base]:-}" ]] && continue
    if [[ -L "$entry" ]]; then
      if $check; then problem "unselected link $base"; else rm -- "$entry"; report "removed unselected link $base"; fi
    else
      problem "unexpected entry .agents/skills/$base (not a link); refusing to remove it"
    fi
  done
fi

# Claude Code reads the same directory.
if [[ -L "$claude_link" ]]; then
  [[ "$(readlink "$claude_link")" == ../.agents/skills ]] ||
    problem ".claude/skills -> $(readlink "$claude_link") (expected ../.agents/skills); refusing to replace it"
elif [[ -e "$claude_link" ]]; then
  problem ".claude/skills is not a symlink; refusing to replace it"
elif $check; then
  problem "missing .claude/skills -> ../.agents/skills"
else
  ln -s ../.agents/skills "$claude_link"
  report "linked .claude/skills -> ../.agents/skills"
fi

if ((problems > 0)); then
  printf 'agent-skills: %d problem(s); %s\n' "$problems" \
    "$($check && echo 'run .agents/link-skills.sh to repair the links' || echo 'resolve the entries above')" >&2
  exit 1
fi
printf 'agent-skills: %d skills linked from %s\n' "${#selected[@]}" "$library"
