package com.mr486.gestojob.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactForm {

    @NotNull(message = "Le nom est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    private String email;
    private Integer formuleDePolitesse = 0;
    private String contact;
}
