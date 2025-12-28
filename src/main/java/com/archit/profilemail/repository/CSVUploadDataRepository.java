package com.archit.profilemail.repository;

import com.archit.profilemail.model.CSVUploadData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CSVUploadDataRepository extends JpaRepository<CSVUploadData,Long> {
    List<CSVUploadData> findByUserAccountEmail(String username);
}
