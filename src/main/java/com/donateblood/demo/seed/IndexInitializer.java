package com.donateblood.demo.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class IndexInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public IndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            IndexOperations ops = mongoTemplate.indexOps("usuarios");

            for (IndexInfo info : ops.getIndexInfo()) {
                boolean soloUsername = info.getIndexFields().size() == 1
                        && "username".equals(info.getIndexFields().get(0).getKey());
                if (soloUsername && !info.isUnique()) {
                    ops.dropIndex(info.getName());
                }
            }

            ops.ensureIndex(new Index().on("username", Sort.Direction.ASC).unique().named("username_1"));
        } catch (Exception e) {
            log.warn("No se pudo crear el índice único de username (¿hay usernames repetidos?): {}", e.getMessage());
        }
    }
}
