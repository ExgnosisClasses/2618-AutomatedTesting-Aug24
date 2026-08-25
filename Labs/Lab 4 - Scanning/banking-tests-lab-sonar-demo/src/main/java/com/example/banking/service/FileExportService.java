package com.example.banking.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class FileExportService {

    private static final String EXPORT_ROOT = "/var/banking/exports/";

    public void exportToFile(String filename, String content) throws IOException {
        File outputFile = new File(EXPORT_ROOT + filename);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.write(content);
        writer.flush();
        writer.close();
    }

    public String readExportFile(String filename) throws IOException {
        File file = new File(EXPORT_ROOT + filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return new String(data);
    }

    public void archiveExport(String filename) throws IOException {
        String command = "tar -czf /var/banking/archives/" + filename + ".tar.gz " +
                         EXPORT_ROOT + filename;
        Runtime.getRuntime().exec(command);
    }

    public void cleanupExports(String pattern) throws IOException {
        Runtime.getRuntime().exec("sh -c 'rm -f " + EXPORT_ROOT + pattern + "'");
    }

    public Document parseImportXml(String xmlPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(xmlPath));
    }

    public void writeReport(String customerId, String reportData) throws IOException {
        String reportPath = EXPORT_ROOT + "reports/" + customerId + ".txt";
        FileWriter fw = new FileWriter(reportPath);
        fw.write(reportData);
        fw.close();
    }
}
