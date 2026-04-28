package healthcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class HealthcareTpApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthcareTpApplication.class, args);
	}

}
