# Security and Git Checklist

Run this checklist before pushing PolicyFlow to any remote repository, especially a public one.

## 1. Inspect tracked and untracked files

From Mac Terminal:

```bash
cd ~/Projects/PolicyFlow
git status --short
git ls-files .env .env.*
git ls-files | grep -E '(^|/)(\.env|.*\.pem|.*\.key|credentials|secrets)(\.|$)' || true
```

If a local environment file is already tracked by Git, adding it to `.gitignore` is not enough by itself; it must be removed from the Git index while leaving the local file in place (for example, `git rm --cached .env` after confirming that is the correct file).

## 2. Search the working tree locally for sensitive configuration

Check Compose files, properties files, scripts, tests, and recent diffs for literal credentials, API tokens, private keys, or personal connection strings. Do this locally; do not paste any matching secret values into chat or a ticket.

```bash
git diff --check
git status --short
git diff -- . ':!*.lock'
git diff --cached -- . ':!*.lock'
```

Also inspect Compose environment variables. Prefer references to variables (for example `${DB_PASSWORD}`) and a local ignored `.env` file rather than literal secrets in committed YAML. Be cautious: `docker compose config` resolves variables and can print secret values.

## 3. Ensure secret files are ignored

Inspect the existing `.gitignore` before editing it. At minimum, typical local exclusions include:

```gitignore
.env
.env.*
!.env.example
.DS_Store
**/target/
**/node_modules/
**/dist/
.idea/
```

Do not blindly replace an existing `.gitignore`; merge these entries with current project rules. If the project needs an `.env.example`, include variable names and safe placeholders only, never real credentials.

## 4. Review before committing

```bash
git status --short
git diff --check
git diff --stat
git diff
```

Never share a terminal screenshot or raw diagnostic output if it includes passwords, tokens, authorization headers, private keys, or expanded Compose environment variables.

## 5. If a secret was committed

Deleting the line in a later commit does not remove the secret from Git history. Revoke or rotate the credential first, then follow the hosting provider's procedure to purge sensitive history if needed. Do not assume a local-only password is safe to reuse elsewhere.

## 6. Check the documentation before push

These Markdown files intentionally avoid embedding passwords and tokens. Re-read them and verify that endpoint examples, port numbers, commands, and statements about test coverage still match the repository you are actually pushing.
