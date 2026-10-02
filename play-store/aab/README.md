# Hand-off bundles

The signed AAB for the release in flight lands here, named
`quran-<version>-vc<code>.aab`. The bundle is a large binary and never enters
the repository (`.gitignore` covers this folder), and it is deleted once Play
has it; the checksum and size are handed over in chat with the release notes.
