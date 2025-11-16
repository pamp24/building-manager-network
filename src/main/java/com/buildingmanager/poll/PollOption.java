package com.buildingmanager.poll;



import com.buildingmanager.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "poll_options")
public class PollOption extends BaseEntity {

    @Column(name = "option_number")
    private Integer number;

    private String text;
    private int votes = 0;

    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    private Poll poll;


}
