# Changelog

Notable changes to Vaikaparts Domain are documented here.

## Unreleased

### Fixed

- Make demand and offer creation idempotent when clients retry a request after a lost response.
- Accept HEIC and HEIF profile photos, including uploads whose browser MIME type is missing or generic; store them with the matching MIME type and file extension.
- Disable verbose Hibernate SQL statement logging in the preproduction profile.
