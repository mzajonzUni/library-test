package pl.zajonz.librarytest.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import pl.zajonz.librarytest.service.UserServiceImpl;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final UserServiceImpl userService;


    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll();
                    auth.requestMatchers(HttpMethod.PATCH, "/api/v1/categories/{id}/subscribe").hasRole("CUSTOMER");
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/users/{id}/books").authenticated();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasRole("EMPLOYEE");
                    auth.requestMatchers("/h2").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/books").hasRole("EMPLOYEE");
                    auth.requestMatchers("/api/v1/books/{id}/block").hasRole("EMPLOYEE");
                    auth.requestMatchers("/api/v1/books/{id}/borrow").hasRole("CUSTOMER");
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .userDetailsService(userService)
                .httpBasic(Customizer.withDefaults())
                .headers().frameOptions().disable()
                .and()
                .httpBasic(Customizer.withDefaults())
                .build();
    }

}
