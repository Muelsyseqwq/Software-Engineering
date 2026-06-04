package com.nekocafe.cat.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CatService {

    private final List<CatProfile> cats = new ArrayList<>(List.of(
        new CatProfile(1L, "拿铁", "英短", 3, "MALE", "亲人、安静", "健康", "", "适合陪伴预约顾客互动", "ACTIVE"),
        new CatProfile(2L, "布丁", "布偶", 2, "FEMALE", "活泼、爱玩", "健康", "", "适合亲子互动", "ACTIVE")
    ));

    public List<CatProfile> listCats() {
        return cats;
    }

    public CatProfile getCat(Long id) {
        return cats.stream()
            .filter(cat -> cat.id().equals(id))
            .findFirst()
            .orElse(null);
    }

    public CatProfile createCat(CatProfile payload) {
        long nextId = cats.stream().mapToLong(CatProfile::id).max().orElse(0L) + 1;
        CatProfile created = new CatProfile(nextId, payload.name(), payload.breed(), payload.age(), payload.gender(), payload.personality(), payload.healthStatus(), payload.photoUrl(), payload.description(), payload.status());
        cats.add(created);
        return created;
    }

    public CatProfile updateCat(Long id, CatProfile payload) {
        CatProfile updated = new CatProfile(id, payload.name(), payload.breed(), payload.age(), payload.gender(), payload.personality(), payload.healthStatus(), payload.photoUrl(), payload.description(), payload.status());
        cats.removeIf(cat -> cat.id().equals(id));
        cats.add(updated);
        return updated;
    }

    public void deleteCat(Long id) {
        cats.removeIf(cat -> cat.id().equals(id));
    }

    public record CatProfile(Long id, String name, String breed, Integer age, String gender, String personality, String healthStatus, String photoUrl, String description, String status) {
    }
}
