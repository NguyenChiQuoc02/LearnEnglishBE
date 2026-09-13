package com.personal.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.net.Socket;

@SpringBootApplication
public class BaseApplication {

	public static void main(String[] args) {
		checkTcpReachability("gateway01.ap-southeast-1.prod.aws.tidbcloud.com", 4000);
		SpringApplication.run(BaseApplication.class, args);
	}

	// Temporary outbound-network diagnostic for the Render deploy — safe to delete once the
	// TiDB connection issue is resolved.
	private static void checkTcpReachability(String host, int port) {
		System.out.println("[net-check] connecting to " + host + ":" + port + " ...");
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 5000);
			System.out.println("[net-check] CONNECTED to " + host + ":" + port);
		} catch (Exception e) {
			System.out.println("[net-check] FAILED to reach " + host + ":" + port + " -> " + e);
		}
	}

}
