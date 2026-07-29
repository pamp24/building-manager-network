package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommonExpenseStatementControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CommonExpenseStatementService service;

    @Mock
    private BuildingRepository buildingRepository;

    @InjectMocks
    private CommonExpenseStatementController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getById_returnsStatement() throws Exception {
        CommonExpenseStatement entity = CommonExpenseStatement.builder()
                .id(1)
                .code("STMT-001")
                .month("2026-07")
                .subTotal(BigDecimal.valueOf(500))
                .total(BigDecimal.valueOf(620))
                .allocations(List.of())
                .items(List.of())
                .build();
        entity.setBuilding(Building.builder().id(1).build());

        when(service.getById(1)).thenReturn(entity);

        mockMvc.perform(get("/expenses/statements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("STMT-001"))
                .andExpect(jsonPath("$.month").value("2026-07"));
    }

    @Test
    void getById_notFound_returnsError() throws ServletException {
        when(service.getById(999)).thenThrow(new RuntimeException("Δεν βρέθηκε κατάσταση με id 999"));

        assertThrows(ServletException.class, () ->
            mockMvc.perform(get("/expenses/statements/999"))
        );
    }

    @Test
    void getAll_returnsList() throws Exception {
        CommonExpenseStatement s1 = CommonExpenseStatement.builder().id(1).code("S1").allocations(List.of()).items(List.of()).build();
        s1.setBuilding(Building.builder().id(1).build());
        CommonExpenseStatement s2 = CommonExpenseStatement.builder().id(2).code("S2").allocations(List.of()).items(List.of()).build();
        s2.setBuilding(Building.builder().id(1).build());

        when(service.getAll()).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/expenses/statements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deleteStatement_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/expenses/statements/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createAndSend_returnsCreatedStatement() throws Exception {
        CommonExpenseStatementDTO request = CommonExpenseStatementDTO.builder()
                .code("NEW-001")
                .month("2026-07")
                .type("ΜΗΝΙΑΙΟ")
                .discountPercent(BigDecimal.ZERO)
                .taxPercent(BigDecimal.valueOf(24))
                .build();

        Building building = Building.builder().id(1).build();

        CommonExpenseStatement savedEntity = CommonExpenseStatement.builder()
                .id(1)
                .code("NEW-001")
                .month("2026-07")
                .subTotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .allocations(List.of())
                .items(List.of())
                .build();
        savedEntity.setBuilding(building);

        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));
        when(service.createAndSend(any())).thenReturn(savedEntity);

        mockMvc.perform(post("/expenses/statements/1/createAndSend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("NEW-001"));
    }

    @Test
    void createAndSend_buildingNotFound_returnsError() {
        CommonExpenseStatementDTO request = CommonExpenseStatementDTO.builder()
                .code("NEW-001")
                .month("2026-07")
                .build();

        when(buildingRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ServletException.class, () ->
            mockMvc.perform(post("/expenses/statements/999/createAndSend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
        );
    }

    @Test
    void saveDraft_returnsDraftStatement() throws Exception {
        CommonExpenseStatementDTO request = CommonExpenseStatementDTO.builder()
                .code("DRAFT-001")
                .month("2026-07")
                .build();

        CommonExpenseStatement savedDraft = CommonExpenseStatement.builder()
                .id(1)
                .code("DRAFT-001")
                .month("2026-07")
                .status(StatementStatus.DRAFT)
                .allocations(List.of())
                .items(List.of())
                .build();
        savedDraft.setBuilding(Building.builder().id(1).build());

        when(service.saveDraft(any())).thenReturn(savedDraft);

        mockMvc.perform(post("/expenses/statements/1/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DRAFT-001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getNextCode_returnsGeneratedCode() throws Exception {
        when(service.generateNextCode(1)).thenReturn("20260726-000001-000001");

        mockMvc.perform(get("/expenses/statements/next-code/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("20260726-000001-000001"));
    }

    @Test
    void getStatementsByBuilding_returnsStatements() throws Exception {
        CommonExpenseStatementDTO dto1 = CommonExpenseStatementDTO.builder().id(1).code("S1").build();
        CommonExpenseStatementDTO dto2 = CommonExpenseStatementDTO.builder().id(2).code("S2").build();

        when(service.getStatementsByBuilding(1)).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/expenses/statements/building/1/statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getActiveStatementsByBuilding_returnsActiveStatements() throws Exception {
        CommonExpenseStatementDTO dto = CommonExpenseStatementDTO.builder().id(1).code("ACTIVE").build();

        when(service.getActiveStatementsByBuildingDTO(1)).thenReturn(List.of(dto));

        mockMvc.perform(get("/expenses/statements/building/1/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ACTIVE"));
    }

    @Test
    void updateStatement_returnsUpdated() throws Exception {
        CommonExpenseStatementDTO updateDto = CommonExpenseStatementDTO.builder()
                .code("UPDATED")
                .month("2026-08")
                .discountPercent(BigDecimal.ZERO)
                .taxPercent(BigDecimal.valueOf(24))
                .subTotal(BigDecimal.valueOf(100))
                .total(BigDecimal.valueOf(124))
                .build();

        when(service.updateStatement(eq(1), any())).thenReturn(updateDto);

        mockMvc.perform(put("/expenses/statements/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATED"));
    }
}
