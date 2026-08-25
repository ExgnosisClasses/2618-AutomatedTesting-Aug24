package com.example.banking.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class DocumentService {

    private static final String DOCUMENTS_DIR = "/var/banking/documents/";

    /**
     * Read a customer document by filename.
     *
     * VULNERABILITY: Path traversal. The filename parameter is concatenated
     * into a file path with no validation. An attacker supplying
     *   "../../etc/passwd"
     * can read arbitrary files outside the documents directory. On Windows,
     * "..\..\..\Windows\System32\config\SAM" is the equivalent.
     *
     * The fix is to validate that the filename contains no path separators
     * (no "/", no "\", no ".."), or to canonicalize the resolved path and
     * verify it remains within DOCUMENTS_DIR.
     */
    public byte[] readCustomerDocument(String filename) throws IOException {
        File documentFile = new File(DOCUMENTS_DIR + filename);
        return Files.readAllBytes(documentFile.toPath());
    }
}
