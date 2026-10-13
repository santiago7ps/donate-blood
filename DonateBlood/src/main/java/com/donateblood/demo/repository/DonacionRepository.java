package com.donateblood.demo.repository;

import com.donateblood.demo.model.Donacion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DonacionRepository
        extends MongoRepository<Donacion, String> {
}
