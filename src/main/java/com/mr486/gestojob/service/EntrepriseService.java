package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.dto.EntrepriseListe;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;

    @Value("${gestojob.max-entreprises-par-page}")
    private int maxEntreprisesParPage;

    public static void Convert(EntrepriseForm form, Entreprise e) {
        e.setNom(form.getNom());
        e.setEstActive(form.getEstActive());
        e.setAdresse1(form.getAdresse1());
        e.setAdresse2(form.getAdresse2());
        e.setCodePostal(form.getCodePostal());
        e.setVille(form.getVille());
    }

    public int save(EntrepriseForm form) {
        if (entrepriseRepository.existsByNomIgnoreCase(form.getNom())) {
            throw new RuntimeException("L'entreprise existe déjà");
        }
        Entreprise entreprise = entrepriseRepository.save(form.entity(form));
        return entreprise.getId();
    }

    public void update(Integer entrepriseId, EntrepriseForm form) {
        try {
            Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
            Convert(form, e);
            entrepriseRepository.save(e);
        } catch (Exception ex) {
            throw new RuntimeException("Entreprise introuvable avec id: " + entrepriseId);
        }
    }

    public EntrepriseForm getForm(Integer entrepriseId) {
        return new EntrepriseForm(entrepriseRepository.findById(entrepriseId).orElseThrow());
    }

    public Entreprise getEntreprise(Integer entrepriseId) {
        return entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec id: " + entrepriseId));
    }

    /**
     * Charge en une seule requête toutes les entreprises demandées, indexées par id.
     * Évite le problème N+1 lors de la construction des listes d'annonces.
     */
    public Map<Integer, Entreprise> getEntreprisesByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return entrepriseRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Entreprise::getId, Function.identity()));
    }

    public void desactiveEntreprise(Integer entrepriseId) {
        Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
        e.setEstActive(false);
        entrepriseRepository.save(e);
    }

    public void activeEntreprise(Integer entrepriseId) {
        Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
        e.setEstActive(true);
        entrepriseRepository.save(e);
    }

    public Page<EntrepriseListe> getAllListePage(int pageIndex) {
        int safePageIndex = Math.max(0, pageIndex);

        return entrepriseRepository
                .findAllByOrderByNomAsc(PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(e -> EntrepriseListe.builder()
                        .id(e.getId())
                        .nom(e.getNom())
                        .estActive(e.getEstActive())
                        .build());
    }

    public List<EntrepriseListe> rechercheEntrepriseParNom(String nom) {
        return entrepriseRepository.findAllByNomContainingIgnoreCase(nom)
                .stream()
                .map(e -> EntrepriseListe.builder()
                        .id(e.getId())
                        .nom(e.getNom())
                        .estActive(e.getEstActive())
                        .build())
                .collect(Collectors.toList());
    }

    public List<EntrepriseListe> rechercheEntrepriseActive() {
        return entrepriseRepository.findAllByEstActiveTrue()
                .stream()
                .map(e -> EntrepriseListe.builder()
                        .id(e.getId())
                        .nom(e.getNom())
                        .estActive(e.getEstActive())
                        .build())
                .collect(Collectors.toList());
    }

    public Boolean existe(Integer entrepriseId) {
        return entrepriseRepository.existsById(entrepriseId);
    }

    public Integer countAllEntreprises() {
        return (int) entrepriseRepository.count();
    }
}
