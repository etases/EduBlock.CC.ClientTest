package me.hsgamer.edublock.cc.clienttest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Subject {
    String name;
    float firstHalfScore;
    float secondHalfScore;
    float finalScore;
}
