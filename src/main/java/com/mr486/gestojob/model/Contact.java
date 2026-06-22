package com.mr486.gestojob.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "contacts")
public class Contact {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private Integer entrepriseId;
    @NotNull
    private String email;
    private Integer formuleDePolistesse;
    private String contact;

    public String getMessageDePolitesse() {
        String message = "";
        if (formuleDePolistesse == 2) {
            message = "Madame " + contact + ",";
        } else if (formuleDePolistesse == 1) {
            message = "Monsieur " + contact + ",";
        } else {
            message = "Madame, Monsieur,";
        }

        return message;
    }
}
