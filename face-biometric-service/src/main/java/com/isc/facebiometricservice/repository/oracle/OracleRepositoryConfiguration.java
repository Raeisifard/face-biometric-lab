package com.isc.facebiometricservice.repository.oracle;

import com.isc.facebiometricservice.config.BiometricProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name="biometric.repository-type", havingValue="oracle")
public class OracleRepositoryConfiguration {
    @Bean DataSource dataSource(BiometricProperties p){
        DriverManagerDataSource ds=new DriverManagerDataSource();
        ds.setDriverClassName("oracle.jdbc.OracleDriver"); ds.setUrl(p.oracle().url()); ds.setUsername(p.oracle().username()); ds.setPassword(p.oracle().password()); return ds;
    }
    @Bean JdbcTemplate jdbcTemplate(DataSource ds){return new JdbcTemplate(ds);}
}
