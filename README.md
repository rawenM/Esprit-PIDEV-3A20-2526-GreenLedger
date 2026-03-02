# Pi_Dev

Minimal notes for the UI workspaces in this repo.

## Run

Use your IDE run configuration for `org.GreenLedger.MainFX`.

## Verify

```powershell
mvn -q test
```

## DynamicPDF export

PDF export uses DynamicPDF when `DPDF_API_KEY` is set; otherwise it falls back to local PDFBox.

Required env (either in `.env` at repo root or system env):
- `DPDF_API_KEY`

Optional env:
- `DPDF_API_URL` (default: `https://api.dpdf.io/v1.0/pdf`)
- `DPDF_AUTH_HEADER` (default: `Authorization`)
- `DPDF_AUTH_SCHEME` (default: `Bearer`)
- `DPDF_TIMEOUT_MS` (default: `15000`)
- `DPDF_REQUEST_MODE` (`html`, `json`, `json-alt`, `json-doc`, `template`, or `auto`; default: `html`)
- `DPDF_REQUEST_TEMPLATE_PATH` (used when `DPDF_REQUEST_MODE=template`)

Template placeholders (when using `DPDF_REQUEST_TEMPLATE_PATH`):
- `${HTML}`: HTML escaped for JSON
- `${HTML_BASE64}`: Base64-encoded HTML

Quick smoke test (run from IDE):
- Main class: `tools.DynamicPdfTestMain`

Optional CLI (requires exec-maven-plugin):

```powershell
mvn -q -DskipTests exec:java -Dexec.mainClass=tools.DynamicPdfTestMain
```
