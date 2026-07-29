package com.buildingmanager.buildingDocument;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(
        "/buildings/{buildingId}/documents"
)
@RequiredArgsConstructor
public class BuildingDocumentController {

    private final BuildingDocumentService documentService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public List<BuildingDocumentDTO> uploadDocuments(
            @PathVariable Integer buildingId,

            @RequestPart("files")
            List<MultipartFile> files,

            @RequestParam(
                    value = "category",
                    required = false
            )
            BuildingDocumentCategory category,

            Authentication authentication
    ) {
        return documentService.uploadDocuments(
                buildingId,
                files,
                category,
                authentication
        );
    }

    @GetMapping
    public List<BuildingDocumentDTO> getDocuments(
            @PathVariable Integer buildingId,
            Authentication authentication
    ) {
        System.out.println(
                "GET DOCUMENTS FOR BUILDING: "
                        + buildingId
        );

        return documentService.getDocuments(
                buildingId,
                authentication
        );
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable Integer buildingId,
            @PathVariable Integer documentId,
            Authentication authentication
    ) {
        documentService.deleteDocument(
                buildingId,
                documentId,
                authentication
        );
    }
}
