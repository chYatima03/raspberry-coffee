/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rpi.sensors.se;

import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jsonp.server.JsonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;
import rpi.sensors.ADCChannel;
import rpi.sensors.RelayManager;

import java.io.IOException;
import java.util.logging.LogManager;

/**
 * Simple Hello World rest application.
 */
public final class Main {

	/**
	 * Cannot be instantiated.
	 */
	private Main() {
	}

	/**
	 * Application main entry point.
	 *
	 * @param args command line arguments.
	 * @throws IOException if there are problems reading logging properties
	 */
	public static void main(final String[] args) throws IOException {
		System.out.println("Starting the SE Server");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nArgh! Freeing resources");
			ADCChannel.close();
			RelayManager.shutdown();
			System.out.println("Resources released.");
		}));
		startServer();
	}

	/**
	 * Start the server.
	 *
	 * @return the created {@link WebServer} instance
	 * @throws IOException if there are problems reading logging properties
	 */
	static WebServer startServer() throws IOException {

		// load logging configuration
		LogManager.getLogManager().readConfiguration(
				Main.class.getResourceAsStream("/logging.properties"));

		// By default this will pick up application.yaml from the classpath
		Config config = Config.create();

		// Get webserver config from the "server" section of application.yaml
		ServerConfiguration serverConfig =
				ServerConfiguration.create(config.get("server"));

		WebServer server = WebServer.create(serverConfig, createRouting(config));

		// Try to start the server. If successful, print some info and arrange to
		// print a message at shutdown. If unsuccessful, print the exception.
		server.start()
				.thenAccept(ws -> {
					System.out.println(">> WEB server is up! http://localhost:" + ws.port() + "/greet");
					System.out.println(">>          Also try http://localhost:" + ws.port() + "/health");
					System.out.println(">>               and http://localhost:" + ws.port() + "/metrics");
					ws.whenShutdown().thenRun(() -> {
						System.out.println("WEB server is DOWN. Good bye!");
						System.out.println("Free resources here.");
					});
				})
				.exceptionally(t -> {
					System.err.println("Argh! Startup failed: " + t.getMessage());
					t.printStackTrace(System.err);
					return null;
				});

		// Server threads are not daemons. No need to block. Just react.

		return server;
	}

	/**
	 * Creates new {@link Routing}.
	 *
	 * @param config configuration of this server
	 * @return routing configured with JSON support, a health check, and a service
	 */
	private static Routing createRouting(Config config) {

		MetricsSupport metrics = MetricsSupport.create();
		GreetService greetService = new GreetService(config);

		SensorService sensorService = new SensorService(config);
		RelayService relayService = new RelayService(config);

		HealthSupport health = HealthSupport.builder()
				.add(HealthChecks.healthChecks())   // Adds a convenient set of checks
				.build();

		return Routing.builder()
				.register(JsonSupport.create())
				.register(health)                   // Health at "/health"
				.register(metrics)                  // Metrics at "/metrics"
				.register("/greet", greetService) // We leave this one, as an example.
				// Extra resources would go here
				.register("/sensors", sensorService)
				.register("/sensors", relayService)

				.build();
	}

}
