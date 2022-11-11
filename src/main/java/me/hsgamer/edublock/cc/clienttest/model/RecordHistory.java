package me.hsgamer.edublock.cc.clienttest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RecordHistory {
    Date timestamp;
    Record record;
    String updatedBy;
}
