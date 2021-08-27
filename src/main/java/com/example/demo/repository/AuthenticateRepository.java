package com.example.demo.repository;

import com.example.demo.entity.P2PTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Authenticate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AuthenticateRepository extends JpaRepository<Authenticate,String>{

    @Modifying
    @Query(value="alter table Authenticate add parition(partition :date values less than(':compare')", nativeQuery = true)
    public void addPartition(String date, String compare);
}
