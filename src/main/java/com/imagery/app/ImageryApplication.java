package com.imagery.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@SpringBootApplication
public class ImageryApplication {

	public static Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName("org.postgresql.Driver");
		return DriverManager.getConnection("jdbc:postgresql://localhost:5432/vkr_db", "postgres", "1");
	}

	public static void main(String[] args) {
		SpringApplication.run(ImageryApplication.class, args);
	}

}
