package com.archit.profilemail.controller;

import com.archit.profilemail.dto.RegisterRequest;
import com.archit.profilemail.model.User;
import com.archit.profilemail.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/")
    public void home(HttpServletResponse response) {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        System.out.println("Hello Archit");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest req) {
        //We don't use @RequestBody User user directly here because of following reasons:
        //authService.registerUser(user)
        //1. If your User entity has extra fields like id, roles, or createdAt, users might send unexpected data.
        //   Ex - End user sends data with role = Admin, which was not intended but now saved in DB
        //2. If we modify the User entity (e.g., add a new column), it could break our API because our API directly depends on the entity.
        //3. Without proper validation, a user might send incomplete data, and our application might crash.

        //Better option is to use DTO which contains the fields we need in order to register our user
        User newUser = new User();
        newUser.setEmail(req.getEmail());
        System.out.println(req.getEmail());
        newUser.setPassword(req.getPassword());
        authService.registerUser(newUser);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/user")
    public ResponseEntity<User> user(@RequestBody String email) {
        return ResponseEntity.ok(authService.findUserByEmail(email));
    }


}
