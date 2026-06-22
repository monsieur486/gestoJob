package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findAllByEntrepriseId(Integer id);
}
