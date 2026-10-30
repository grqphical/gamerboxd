package com.gamerboxd.gamerboxd.repositories;

import com.gamerboxd.gamerboxd.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

}
