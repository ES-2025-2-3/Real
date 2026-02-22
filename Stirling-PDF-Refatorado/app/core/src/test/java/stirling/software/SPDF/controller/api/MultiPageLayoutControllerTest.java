package stirling.software.SPDF.controller.api;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import stirling.software.SPDF.model.api.general.MergeMultiplePagesRequest;
import stirling.software.common.service.CustomPDFDocumentFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiPageLayoutController Tests")
class MultiPageLayoutControllerTest {

    @Mock private CustomPDFDocumentFactory pdfDocumentFactory;

    @InjectMocks private MultiPageLayoutController controller;

    private MockMultipartFile fileWithExt;
    private MockMultipartFile fileNoExt;

    @BeforeEach
    void setup() {
        fileWithExt =
                new MockMultipartFile(
                        "fileInput", "test.pdf", "application/pdf", new byte[] {1, 2, 3});
        fileNoExt =
                new MockMultipartFile("fileInput", "name", "application/pdf", new byte[] {4, 5, 6});
    }

    @Test
    @DisplayName("Rejects non-2/3 and non-perfect-square pagesPerSheet")
    void invalidPagesPerSheetThrows() {
        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(5);
        req.setAddBorder(Boolean.TRUE);
        req.setFileInput(fileWithExt);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> controller.mergeMultiplePagesIntoOne(req));
    }

    @Test
    @DisplayName("Generates PDF and filename suffix for perfect-square layout with no source pages")
    void perfectSquareNoPages() throws Exception {
        PDDocument source = new PDDocument();
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileWithExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(4);
        req.setAddBorder(Boolean.FALSE);
        req.setFileInput(fileWithExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertEquals(MediaType.APPLICATION_PDF, resp.getHeaders().getContentType());
        Assertions.assertNotNull(resp.getBody());
        Assertions.assertTrue(resp.getBody().length > 0);
        Assertions.assertEquals(
                "test_multi_page_layout.pdf",
                resp.getHeaders().getContentDisposition().getFilename());
    }

    @Test
    @DisplayName("Merges single source page into 2-up layout and returns PDF")
    void twoUpWithSinglePage() throws Exception {
        PDDocument source = new PDDocument();
        source.addPage(new PDPage());
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileWithExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(2);
        req.setAddBorder(Boolean.TRUE);
        req.setFileInput(fileWithExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertEquals(MediaType.APPLICATION_PDF, resp.getHeaders().getContentType());
        Assertions.assertNotNull(resp.getBody());
        Assertions.assertTrue(resp.getBody().length > 0);
    }

    @Test
    @DisplayName("Uses input name without extension and appends suffix for 3-up")
    void threeUpWithNameNoExtension() throws Exception {
        PDDocument source = new PDDocument();
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileNoExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(3);
        req.setAddBorder(Boolean.TRUE);
        req.setFileInput(fileNoExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(
                "name_multi_page_layout.pdf",
                resp.getHeaders().getContentDisposition().getFilename());
    }

    // ---------------- Additional tests ----------------

    @Test
    @DisplayName("Batches pages into multiple output pages when pagesPerSheet < totalPages (2-up)")
    void multipleOutputPagesTwoUp() throws Exception {
        PDDocument source = new PDDocument();
        // 5 source pages
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileWithExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(2);
        req.setAddBorder(Boolean.FALSE);
        req.setFileInput(fileWithExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertNotNull(resp.getBody());

        try (PDDocument out = Loader.loadPDF(resp.getBody())) {
            // ceil(5 / 2) = 3 output pages
            Assertions.assertEquals(3, out.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Draws borders for each cell when addBorder=true")
    void addsBordersWhenRequested() throws Exception {
        PDDocument source = new PDDocument();
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileWithExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(2);
        req.setAddBorder(Boolean.TRUE);
        req.setFileInput(fileWithExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertNotNull(resp.getBody());
        try (PDDocument out = Loader.loadPDF(resp.getBody())) {
            Assertions.assertEquals(1, out.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Skips form field copy when any source page is rotated")
    void skipsFormCopyWhenRotated() throws Exception {
        PDDocument source = new PDDocument();
        PDPage p = new PDPage();
        p.setRotation(90); // rotated page should trigger skip path
        source.addPage(p);
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileWithExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(2);
        req.setAddBorder(Boolean.FALSE);
        req.setFileInput(fileWithExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertNotNull(resp.getBody());
    }

    @Test
    @DisplayName("Non-rotated source exercises form copy path without throwing")
    void nonRotatedFormCopyPath() throws Exception {
        PDDocument source = new PDDocument();
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileWithExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(2);
        req.setAddBorder(Boolean.FALSE);
        req.setFileInput(fileWithExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        try (PDDocument out = Loader.loadPDF(resp.getBody())) {
            Assertions.assertEquals(1, out.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("3-up layout with multiple pages produces valid PDF and correct page count")
    void threeUpMultiplePagesProducesValidPdf() throws Exception {
        PDDocument source = new PDDocument();
        // 5 pages -> ceil(5/3)=2 output pages
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        source.addPage(new PDPage());
        PDDocument target = new PDDocument();
        Mockito.when(pdfDocumentFactory.load(fileWithExt)).thenReturn(source);
        Mockito.when(pdfDocumentFactory.createNewDocumentBasedOnOldDocument(source))
                .thenReturn(target);

        MergeMultiplePagesRequest req = new MergeMultiplePagesRequest();
        req.setPagesPerSheet(3);
        req.setAddBorder(Boolean.FALSE);
        req.setFileInput(fileWithExt);

        ResponseEntity<byte[]> resp = controller.mergeMultiplePagesIntoOne(req);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertEquals(
                "test_multi_page_layout.pdf",
                resp.getHeaders().getContentDisposition().getFilename());
        try (PDDocument out = Loader.loadPDF(resp.getBody())) {
            Assertions.assertEquals(2, out.getNumberOfPages());
        }
    }
}
