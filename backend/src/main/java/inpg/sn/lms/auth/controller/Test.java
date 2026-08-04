package inpg.sn.lms.auth.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Test {
    @GetMapping("")
    public String getMethodName() {
        return "Hello World";
    }

    @GetMapping("/test")
    public String test() {
        return "Hello from AuthController";
    }

    @GetMapping("/voirplus")
    public String voirplus() {
        return "Hello from AuthController";
    }

    @GetMapping("/etudiant")
    public String etudiant() {
        return "Hello from AuthController for Etudiant";
    }
    
}
