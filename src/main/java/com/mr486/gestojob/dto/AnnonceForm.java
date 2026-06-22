package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnonceForm {
    private Integer entrepriseId;
    private Integer contenuId = 0;
    private Long contactId;
    private Integer typeAnnonce = 0;
    private String poste;
    private String reference;
    private OffsetDateTime dateEnvoi;
}
