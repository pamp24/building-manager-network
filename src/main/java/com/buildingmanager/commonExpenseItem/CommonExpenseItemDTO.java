package com.buildingmanager.commonExpenseItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonExpenseItemDTO {

    private Integer id;
    private String category;
    private String descriptionItem;
    private BigDecimal price;


}
