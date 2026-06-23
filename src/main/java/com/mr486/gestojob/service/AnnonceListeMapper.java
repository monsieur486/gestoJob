package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.TypeAnnonce;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assembleur des lignes de liste d'annonces ({@link AnnonceListe}) à partir des
 * entités {@link Annonce}, en résolvant en lot les entreprises et contacts liés
 * pour éviter le problème N+1.
 * <p>
 * Extrait d'{@link AnnonceService} pour isoler la responsabilité de présentation
 * (principe de responsabilité unique).
 */
@Component
@RequiredArgsConstructor
public class AnnonceListeMapper {

    private static final DateTimeFormatter FR_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EntrepriseService entrepriseService;
    private final ContactService contactService;

    /**
     * Convertit une page d'annonces en page de DTO en chargeant les entreprises
     * et contacts liés en lot (évite le N+1).
     *
     * <p><b>Exemple :</b> pour une page d'une annonce, retourne une page d'un AnnonceListe, en conservant pagination et nombre total d'éléments.</p>
     *
     * @param page la page d'annonces à convertir
     * @return la page de DTO correspondante
     */
    public Page<AnnonceListe> toAnnonceListePage(Page<Annonce> page) {
        List<AnnonceListe> content = toAnnonceListe(page.getContent());
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /**
     * Convertit une liste d'annonces en DTO en chargeant en deux requêtes
     * (une pour les entreprises, une pour les contacts) toutes les références.
     *
     * <p><b>Exemple :</b> pour deux annonces liées aux entreprises 1 et 4, charge ces entreprises en une seule requête puis retourne les deux DTO correspondants.</p>
     *
     * @param annonces la liste d'annonces à convertir
     * @return la liste de DTO correspondante
     */
    public List<AnnonceListe> toAnnonceListe(List<Annonce> annonces) {
        Set<Integer> entrepriseIds = annonces.stream()
                .map(Annonce::getEntrepriseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contactIds = annonces.stream()
                .map(Annonce::getContactId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, Entreprise> entreprises = entrepriseService.getEntreprisesByIds(entrepriseIds);
        Map<Long, Contact> contacts = contactService.getContactsByIds(contactIds);

        return annonces.stream()
                .map(a -> fromEntity(a, entreprises, contacts))
                .collect(Collectors.toList());
    }

    // Convertit une annonce en DTO d'affichage en résolvant l'entreprise et le
    // contact depuis les maps pré-chargées, et en formatant date, type et statut.
    private AnnonceListe fromEntity(Annonce annonce, Map<Integer, Entreprise> entreprises,
                                    Map<Long, Contact> contacts) {
        Entreprise entreprise = entreprises.get(annonce.getEntrepriseId());
        Contact contact = annonce.getContactId() != null ? contacts.get(annonce.getContactId()) : null;

        AnnonceListe liste = new AnnonceListe();
        liste.setId(annonce.getId());
        String dateEnvoi = annonce.getDateEnvoi() != null
                ? annonce.getDateEnvoi().format(FR_DATE_TIME)
                : "--";
        liste.setDateEnvoi(dateEnvoi);
        liste.setType(TypeAnnonce.libelleCourt(annonce.getTypeAnnonce()));
        liste.setLibelle(annonce.getLibelle());
        liste.setStatus(annonce.getStatusAnnonceString());
        liste.setInfo(getInfos(annonce, entreprise, contact));
        liste.setEntrepriseId(annonce.getEntrepriseId());
        return liste;
    }

    // Construit la chaîne d'information affichée dans la liste (entreprise, canal
    // de contact email ou site, et type de contenu : MS pour microservices, G sinon).
    private String getInfos(Annonce annonce, Entreprise entreprise, Contact contact) {
        String result = "🏢";
        result += (entreprise != null ? entreprise.getNom() : "?");
        if (annonce.getContactId() != null && contact != null) {
            result += " 🖂" + contact.getEmail();
        } else {
            result += " 🌐site";
        }
        if (annonce.getTypeContenu() != null && annonce.getTypeContenu() == 1) {
            result += " MS";
        } else if (annonce.getTypeContenu() != null && annonce.getTypeContenu() == 2) {
            result += " IA";
        } else {
            result += " G";
        }
        return result;
    }
}
