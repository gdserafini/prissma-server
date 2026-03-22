package br.pucpr.prissma_server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.pucpr.prissma_server.userTest.UserTest;
import br.pucpr.prissma_server.userTest.UserTestRepository;

@SpringBootApplication
@RestController
public class PrissmaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrissmaServerApplication.class, args);
	}

	@GetMapping("/hello-world")
	public String helloWorld() {
		return "Hello World!";
	}

	@Bean
	CommandLineRunner initUserTest(UserTestRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				repository.save(new UserTest("Usuario Teste"));
				System.out.println("UserTest inserido com sucesso!");
			}
		};
	}
}
