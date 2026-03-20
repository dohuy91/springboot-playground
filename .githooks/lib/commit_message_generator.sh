#!/usr/bin/env sh

set -eu

trim() {
  printf "%s" "$1" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

first_line() {
  printf "%s\n" "$1" | sed '/^[[:space:]]*$/d' | head -1
}

extract_message_from_git_commit_command() {
  text="$1"

  # Try: git commit -m "..."
  msg_double="$(printf "%s\n" "$text" | sed -n 's/.*git commit[^\n]*-m "\([^"]*\)".*/\1/p' | head -1)"
  msg_double="$(trim "$msg_double")"
  if [ -n "$msg_double" ]; then
    printf "%s\n" "$msg_double"
    return 0
  fi

  # Try: git commit -m '...'
  msg_single="$(printf "%s\n" "$text" | sed -n "s/.*git commit[^\\n]*-m '\([^']*\)'.*/\\1/p" | head -1)"
  msg_single="$(trim "$msg_single")"
  if [ -n "$msg_single" ]; then
    printf "%s\n" "$msg_single"
    return 0
  fi

  return 1
}

build_ai_prompt() {
  mode="$1"
  subject="$2"
  diff_text="$3"

  cat <<EOF
You are generating a Git commit message.

Rules:
- Output only one single-line commit message.
- Use Conventional Commit style: feat|fix|chore|refactor|test|docs: <summary>
- Max 90 characters.
- No markdown, no quotes, no extra explanation.

Context mode: $mode
Changed files:
$subject

Diff excerpt:
$diff_text
EOF
}

generate_via_custom_ai_command() {
  prompt="$1"
  ai_cmd="${AI_COMMIT_CMD:-}"
  if [ -z "$ai_cmd" ]; then
    return 1
  fi

  # AI_COMMIT_CMD should read prompt from stdin and print a message on stdout.
  out="$(printf "%s\n" "$prompt" | sh -c "$ai_cmd" 2>/dev/null || true)"
  out="$(first_line "$out")"
  out="$(trim "$out")"

  [ -n "$out" ] || return 1
  printf "%s\n" "$out"
}

generate_via_gh_copilot() {
  prompt="$1"
  timeout_seconds="${AI_COMMIT_TIMEOUT_SECONDS:-6}"

  command -v gh >/dev/null 2>&1 || return 1
  command -v perl >/dev/null 2>&1 || return 1
  gh copilot --help >/dev/null 2>&1 || return 1

  tmp_cmd="$(mktemp)"
  tmp_out="$(mktemp)"
  cleanup_tmp() {
    rm -f "$tmp_cmd" "$tmp_out"
  }
  trap cleanup_tmp EXIT INT TERM

  if ! perl -e '$t=shift @ARGV; alarm $t; exec @ARGV' \
      "$timeout_seconds" \
      gh copilot suggest -t git -s "$tmp_cmd" "$prompt" >"$tmp_out" 2>&1; then
    cleanup_tmp
    trap - EXIT INT TERM
    return 1
  fi

  msg=""
  if [ -s "$tmp_cmd" ]; then
    msg="$(extract_message_from_git_commit_command "$(cat "$tmp_cmd")" || true)"
  fi

  if [ -z "$msg" ] && [ -s "$tmp_out" ]; then
    msg="$(extract_message_from_git_commit_command "$(cat "$tmp_out")" || true)"
  fi

  if [ -z "$msg" ]; then
    fallback_line="$(first_line "$(cat "$tmp_out" 2>/dev/null || true)")"
    fallback_line="$(trim "$fallback_line")"
    msg="$fallback_line"
  fi

  cleanup_tmp
  trap - EXIT INT TERM

  [ -n "$msg" ] || return 1
  printf "%s\n" "$msg"
}

generate_via_ai() {
  mode="$1"
  subject="$2"
  diff_text="$3"
  prompt="$(build_ai_prompt "$mode" "$subject" "$diff_text")"

  msg="$(generate_via_custom_ai_command "$prompt" || true)"
  if [ -z "$msg" ]; then
    msg="$(generate_via_gh_copilot "$prompt" || true)"
  fi

  msg="$(first_line "$msg")"
  msg="$(trim "$msg")"

  # If the model returns a full command, extract the message part.
  if printf "%s" "$msg" | grep -q "git commit"; then
    msg="$(extract_message_from_git_commit_command "$msg" || true)"
    msg="$(trim "$msg")"
  fi

  [ -n "$msg" ] || return 1
  printf "%s\n" "$msg"
}

generate_from_names() {
  if [ "$#" -eq 0 ]; then
    echo "chore: empty commit"
    return
  fi

  has_src=0
  has_test=0
  has_docs=0
  has_build=0

  for file in "$@"; do
    case "$file" in
      src/main/*)
        has_src=1
        ;;
      src/test/*|*Test.java)
        has_test=1
        ;;
      *.md|docs/*)
        has_docs=1
        ;;
      build.gradle|settings.gradle|gradle/*|docker-compose.yml|*.yml|*.yaml)
        has_build=1
        ;;
    esac
  done

  kind="chore"
  if [ "$has_src" -eq 1 ]; then
    kind="feat"
  elif [ "$has_test" -eq 1 ] && [ "$has_docs" -eq 0 ] && [ "$has_build" -eq 0 ]; then
    kind="test"
  elif [ "$has_docs" -eq 1 ] && [ "$has_src" -eq 0 ] && [ "$has_test" -eq 0 ] && [ "$has_build" -eq 0 ]; then
    kind="docs"
  fi

  count="$#"
  first_three="$(printf "%s\n" "$@" | head -3 | tr '\n' ', ' | sed 's/, $//')"
  if [ "$count" -eq 1 ]; then
    summary="$first_three"
  else
    summary="$first_three"
  fi

  echo "$kind: update $count file(s) ($summary)"
}

generate_from_staged() {
  files="$(git diff --cached --name-only)"
  diff_excerpt="$(git diff --cached --unified=0 | head -400)"

  ai_msg="$(generate_via_ai "staged" "$files" "$diff_excerpt" || true)"
  if [ -n "$ai_msg" ]; then
    printf "%s\n" "$ai_msg"
    return
  fi

  # shellcheck disable=SC2086
  generate_from_names $files
}

generate_from_commit() {
  commit_sha="$1"
  files="$(git show --pretty="" --name-only "$commit_sha")"
  diff_excerpt="$(git show --pretty="" --unified=0 "$commit_sha" | head -400)"

  ai_msg="$(generate_via_ai "commit" "$files" "$diff_excerpt" || true)"
  if [ -n "$ai_msg" ]; then
    printf "%s\n" "$ai_msg"
    return
  fi

  # shellcheck disable=SC2086
  generate_from_names $files
}

if [ "${1:-}" = "--staged" ]; then
  generate_from_staged
elif [ "${1:-}" = "--commit" ] && [ -n "${2:-}" ]; then
  generate_from_commit "$2"
else
  echo "Usage: $0 --staged | --commit <sha>" >&2
  exit 1
fi
