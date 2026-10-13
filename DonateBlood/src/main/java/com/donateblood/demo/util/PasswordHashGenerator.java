package com.donateblood.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (args.length == 0) {
            System.out.println("Hash de '123456': " + encoder.encode("123456"));
            return;
        }

        for (String password : args) {
            System.out.println(password + " => " + encoder.encode(password));
        }
    }
}