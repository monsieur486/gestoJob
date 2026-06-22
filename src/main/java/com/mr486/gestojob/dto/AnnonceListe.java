package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnonceListe {
    private Long id;
    private Integer entrepriseId;
    private String info;
    private String dateEnvoi;
    private String contact;
    private String type;
    private String libelle;
    private String status;
}
