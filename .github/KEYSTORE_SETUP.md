# Keystore & GitHub Secrets Setup

This project signs the **Release APK** with a `release` keystore configured in `app/build.gradle.kts:47-61` via env vars:

```kotlin
KEYSTORE_FILE       // path to .jks file (decoded in CI)
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

In GitHub Actions the `.jks` is provided as a **base64** secret `KEYSTORE_FILE_B64`. All three other values are plain secrets.

---

## 1) Generate a new keystore (do this ONCE locally)

> If you already have a `my-release-key.jks`, skip generation and go to step 2.

```bash
# JDK's keytool — validity 10000 days (~27 years)
keytool -genkey -v \
  -keystore cinnamon-release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias cinnamon \
  -storepass "YOUR_STRONG_STORE_PASSWORD" \
  -keypass "YOUR_STRONG_KEY_PASSWORD" \
  -dname "CN=Cinnamon, OU=Mobile, O=sosauce, L=Cairo, S=Cairo, C=EG"
```

You'll be asked for passwords — use **strong random** values (e.g. `openssl rand -base64 24`).

**Verify:**
```bash
keytool -list -v -keystore cinnamon-release.jks -alias cinnamon
```

> ⚠️ **Backup `cinnamon-release.jks` + passwords securely** (password manager + offline copy).  
> Losing the keystore means you can **never** update the same app on Play Store / same package name.

---

## 2) Base64-encode the keystore for GitHub

**Linux / macOS:**
```bash
base64 -w 0 cinnamon-release.jks > keystore.b64
# then copy contents
cat keystore.b64 | xclip -selection clipboard   # or pbcopy on macOS
```

**Windows PowerShell (this repo's platform):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("cinnamon-release.jks")) | Set-Content keystore.b64 -NoNewline
Get-Content keystore.b64 | Set-Clipboard
```

**Alternate (Windows cmd using certutil):**
```cmd
certutil -encode cinnamon-release.jks keystore.b64
:: remove -----BEGIN CERTIFICATE----- / -----END CERTIFICATE----- header/footer lines
```

Do **NOT** commit `cinnamon-release.jks` or `keystore.b64` to git. Keep them in `.gitignore`.

---

## 3) Add the 4 GitHub Secrets

Go to: **GitHub → Your Repo → Settings → Secrets and variables → Actions → New repository secret**

Create these **exactly** with these names (workflows expect them):

| Secret Name         | Value                                      | Example |
|---------------------|--------------------------------------------|---------|
| `KEYSTORE_FILE_B64` | Entire base64 string from `keystore.b64` (single line, no header) | `MIIK...very-long...==` |
| `KEYSTORE_PASSWORD` | The `-storepass` you used                  | `s3cr3tStoreP@ss` |
| `KEY_ALIAS`         | The `-alias` you used                      | `cinnamon` |
| `KEY_PASSWORD`      | The `-keypass` you used (often same as store) | `s3cr3tKeyP@ss` |

**Checklist:**
- [ ] `KEYSTORE_FILE_B64` is one **continuous line** (no newlines, no `-----BEGIN...`)
- [ ] `KEY_ALIAS` matches exactly (`cinnamon`)
- [ ] No trailing spaces in any secret value

---

## 4) Test locally (optional)

Simulate CI decoding locally:

```powershell
# PowerShell
[IO.File]::WriteAllBytes("$pwd/test.jks", [Convert]::FromBase64String((Get-Content keystore.b64 -Raw).Trim()))
keytool -list -v -keystore test.jks -alias cinnamon -storepass "YOUR_STOREPASS"
```

```bash
# bash
echo "$YOUR_B64" | base64 -d > /tmp/test.jks
keytool -list -v -keystore /tmp/test.jks -alias cinnamon
```

---

## 5) How CI uses them

Both workflows decode at runtime:

```yaml
- name: Decode keystore
  env:
    KEYSTORE_FILE_B64: ${{ secrets.KEYSTORE_FILE_B64 }}
  run: echo "$KEYSTORE_FILE_B64" | base64 -d > "$PWD/cinnamon-release.jks"

- name: Export signing env
  run: |
    echo "KEYSTORE_FILE=$PWD/cinnamon-release.jks" >> $GITHUB_ENV
    echo "KEYSTORE_PASSWORD=${{ secrets.KEYSTORE_PASSWORD }}" >> $GITHUB_ENV
    echo "KEY_ALIAS=${{ secrets.KEY_ALIAS }}" >> $GITHUB_ENV
    echo "KEY_PASSWORD=${{ secrets.KEY_PASSWORD }}" >> $GITHUB_ENV
```

`app/build.gradle.kts` then picks them up via `System.getenv(...)`. If any is missing the build prints `App won't be signed!` and the APK will be **unsigned** (debug-like).

---

## 6) Creating the `beta` branch

```bash
# from main
git checkout main
git pull origin main
git checkout -b beta
git push -u origin beta
# Now every push to beta triggers the preview prerelease
git checkout main
```

Or create it on GitHub: **Branches → New branch → Source: main → Name: `beta`**

---

## Troubleshooting

- `Could not determine versionName` → Ensure `app/build.gradle.kts` still has `versionName = "x.y.z"`
- `Tag vX.Y.Z already exists` (stable workflow fails) → Bump `versionName` before pushing to `main`
- `Missing secret KEYSTORE_FILE_B64` → Re-check secret name spelling and that you pasted the *full* single-line base64
- APK is unsigned → Check logs for `App won't be signed!` — means one of the 4 env vars was empty
