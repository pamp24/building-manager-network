package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("expenses/statements")
@RequiredArgsConstructor
public class CommonExpenseStatementController {

    private final CommonExpenseStatementService service;
    private final BuildingRepository buildingRepository;

    @PostMapping("/{buildingId}/createAndSend")
    public ResponseEntity<CommonExpenseStatementDTO> createAndSend(
            @PathVariable Integer buildingId,
            @RequestBody CommonExpenseStatementDTO dto
    ) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Building not found"));

        CommonExpenseStatement entity = CommonExpenseStatementMapper.toEntity(dto, buildingId);
        CommonExpenseStatement saved = service.createAndSend(entity);

        return ResponseEntity.ok(CommonExpenseStatementMapper.toDTO(saved));
    }

    @PostMapping("/{buildingId}/draft")
    public ResponseEntity<CommonExpenseStatementDTO> saveDraft(
            @PathVariable Integer buildingId,
            @RequestBody CommonExpenseStatementDTO dto) {
        CommonExpenseStatement statement = CommonExpenseStatementMapper.toEntity(dto, buildingId);
        CommonExpenseStatement saved = service.saveDraft(statement);
        return ResponseEntity.ok(CommonExpenseStatementMapper.toDTO(saved));
    }

    @GetMapping("/expenses/statements")
    public ResponseEntity<List<CommonExpenseStatementDTO>> getAll() {
        List<CommonExpenseStatementDTO> list = service.getAll().stream()
                .map(CommonExpenseStatementMapper::toDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/expenses/statements/active")
    public List<CommonExpenseStatement> getAllActive() {
        return service.getAllActive();
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommonExpenseStatementDTO> getById(@PathVariable Integer id) {
        CommonExpenseStatement entity = service.getById(id);
        return ResponseEntity.ok(CommonExpenseStatementMapper.toDTO(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<CommonExpenseStatementDTO> updateStatement(
            @PathVariable Integer id,
            @RequestBody CommonExpenseStatementDTO dto
    ) {
        CommonExpenseStatementDTO updated = service.updateStatement(id, dto);
        return ResponseEntity.ok(updated);
    }


    @GetMapping("/next-code/{buildingId}")
    public ResponseEntity<String> getNextCode(@PathVariable Integer buildingId) {
        String nextCode = service.generateNextCode(buildingId);
        return ResponseEntity.ok(nextCode);
    }
    @GetMapping("/building/{buildingId}/statement")
    public ResponseEntity<List<CommonExpenseStatementDTO>> getStatementsByBuilding(@PathVariable Long buildingId) {
        List<CommonExpenseStatementDTO> statements =
                service.getStatementsByBuilding(buildingId)
                        .stream()
                        .map(CommonExpenseStatementMapper::toDTO)
                        .toList();
        return ResponseEntity.ok(statements);
    }

}