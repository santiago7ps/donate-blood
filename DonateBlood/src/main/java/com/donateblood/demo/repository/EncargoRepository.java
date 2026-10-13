package com.donateblood.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.donateblood.demo.model.Encargo;

public interface EncargoRepository
        extends MongoRepository<Encargo, String> {

}