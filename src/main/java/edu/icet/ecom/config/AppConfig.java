package edu.icet.ecom.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // ✅ This stops the "multiple source property hierarchies" error
        modelMapper.getConfiguration()
                .setMatchingStrategy(org.modelmapper.convention.MatchingStrategies.STRICT);
        return modelMapper;
    }
}