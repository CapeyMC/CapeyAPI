package hu.jgj52.capeyapi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootApplication
public class CapeyApiApplication {
    public static Dotenv dotenv;
    public static DataSource ds;

    public static void main(String[] args) {
        dotenv = Dotenv.load();
        String dbHost = dotenv.get("POSTGRES_HOST", "127.0.0.1");
        String dbPort = dotenv.get("POSTGRES_PORT", "port");
        String dbDatabase = dotenv.get("POSTGRES_DATABASE", "postgres");
        String dbUser = dotenv.get("POSTGRES_USER", "postgres");
        String dbPassword = dotenv.get("POSTGRES_PASSWORD", "");

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:postgresql://" +
                dbHost + ":" +
                dbPort + "/" +
                dbDatabase
        );
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        ds = new HikariDataSource(config);

        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("""
                CREATE EXTENSION IF NOT EXISTS pgcrypto
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid UUID PRIMARY KEY,
                    cape UUID,
                    token VARCHAR DEFAULT encode(gen_random_bytes(16), 'hex')
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS capes (
                    uuid UUID PRIMARY KEY,
                    uploader UUID,
                    type VARCHAR,
                    timestamp bigint DEFAULT (EXTRACT(epoch FROM now()))::bigint
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        SpringApplication.run(CapeyApiApplication.class, args);
    }

}
