package com.budgetmaster.budgetmaster;

import java.nio.file.Path;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

final class PdfTextExtractor {
    
    String extractText(Path pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile.toFile())) {
            AccessPermission ap = document.getCurrentAccessPermission();
            if (!ap.canExtractContent()) {
                throw new IOException("You do not have permission to extract text");
            }

            PDFTextStripper stripper = new PDFTextStripper();

            stripper.setSortByPosition(sortByPosition);

            String pdfText = stripper.getText(document);

            LOGGER.finest(pdfText);

            return pdfText;
        }
    }
    
    PdfTextExtractor setSortByPosition(boolean v) {
        sortByPosition = v;
        return this;
    }
            
    private boolean sortByPosition;
    
    private static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());
}
