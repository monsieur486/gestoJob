package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Entreprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Integer> {

    Page<Entreprise> findAllByOrderByNomAsc(Pageable pageable);

    List<Entreprise> findAllByNomContainingIgnoreCase(String nom);

    Optional<Entreprise> findById(Integer id);

    Boolean existsByNomIgnoreCase(String nom);

    List<Entreprise> findAllByEstActiveTrue();
}
