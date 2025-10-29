package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.building.Building;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationMapper;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.CommonExpenseItemDTO;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;

import java.util.stream.Collectors;

public class CommonExpenseStatementMapper {

    public static CommonExpenseStatementDTO toDTO(CommonExpenseStatement entity) {
        return CommonExpenseStatementDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .type(entity.getType())
                .month(entity.getMonth())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .discountPercent(entity.getDiscountPercent())
                .taxPercent(entity.getTaxPercent())
                .subTotal(entity.getSubTotal())
                .total(entity.getTotal())
                .description(entity.getDescription())
                .sequenceNumber(entity.getSequenceNumber())
                .status(entity.getStatus())
                .isPaid(entity.getIsPaid())
                .buildingId(entity.getBuilding().getId())
                .items(entity.getItems().stream().map(CommonExpenseStatementMapper::toItemDTO).collect(Collectors.toList()))
                .active(entity.isActive())
                .allocations(entity.getAllocations()
                        .stream()
                        .map(CommonExpenseAllocationMapper::toDTO)
                        .collect(Collectors.toList()))

                .build();
    }

    public static CommonExpenseStatement toEntity(CommonExpenseStatementDTO dto, Integer buildingId) {
        CommonExpenseStatement entity = new CommonExpenseStatement();
        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setType(dto.getType());
        entity.setMonth(dto.getMonth());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setDiscountPercent(dto.getDiscountPercent());
        entity.setTaxPercent(dto.getTaxPercent());
        entity.setSubTotal(dto.getSubTotal());
        entity.setTotal(dto.getTotal());
        entity.setDescription(dto.getDescription());
        entity.setSequenceNumber(dto.getSequenceNumber());
        entity.setStatus(dto.getStatus());
        entity.setIsPaid(dto.getIsPaid());

        Building building = new Building();
        building.setId(buildingId);
        entity.setBuilding(building);

        if (dto.getItems() != null) {
            entity.setItems(dto.getItems().stream()
                    .map(i -> toItemEntity(i, entity))
                    .collect(Collectors.toList()));
        }

        return entity;
    }

    private static CommonExpenseItemDTO toItemDTO(CommonExpenseItem item) {
        return CommonExpenseItemDTO.builder()
                .id(item.getId())
                .category(item.getCategory() != null ? item.getCategory().name() : null) // Enum -> String
                .descriptionItem(item.getDescriptionItem())
                .price(item.getPrice())
                .build();
    }

    private static CommonExpenseItem toItemEntity(CommonExpenseItemDTO dto, CommonExpenseStatement statement) {
        return CommonExpenseItem.builder()
                .id(dto.getId())
                .category(dto.getCategory() != null ? ExpenseCategory.valueOf(dto.getCategory()) : null) // String -> Enum
                .descriptionItem(dto.getDescriptionItem())
                .price(dto.getPrice())
                .statement(statement)
                .build();
    }
}
