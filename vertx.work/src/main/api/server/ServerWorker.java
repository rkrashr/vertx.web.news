package api.server;

import api.endpoints.Stats;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class ServerWorker extends AbstractVerticle {

	private static String DEFAULT_HOST = "0.0.0.0";
	private static int DEFAULT_PORT = 8080;

	private String host;
	private int port;

	private HttpServer server;

	public void start(Future<Void> startFuture) {

		configure()
		.compose(v -> this.makeRoutes())
		.compose(router -> this.startServer(router))
		.setHandler(startFuture.completer());
	}
	
	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		
		server.close(res -> {
			if (res.succeeded()) {
				System.out.println("** stopped **");
				stopFuture.complete();
			}
			else {
				stopFuture.fail(res.cause());
			}
		});
		
		super.stop(stopFuture);
	}



	private Future<Void> configure() {
		
		Future<Void> future = Future.future();
		
		ConfigStoreOptions fileStore = new ConfigStoreOptions()
				.setType("file")
				.setConfig(new JsonObject().put("path", "vertx.app.json"));
		
		ConfigRetrieverOptions options = 
				new ConfigRetrieverOptions()
				.addStore(fileStore);

		ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

		retriever.getConfig(ar -> {
			if (ar.failed()) {
				future.fail(ar.cause());
			} else {
				JsonObject config = ar.result();
			    host = config.getString("address", DEFAULT_HOST);
			    port = config.getInteger("port", DEFAULT_PORT);
			    System.out.println("** configured **");
			    future.complete();
			}
		});

		return future;
	}
	
	
	private Future<Router> makeRoutes() {
		System.out.println("** configure routes **");
		
		Future<Router> future = Future.future();
		
		Router router = Router.router(vertx);
		
		router.route(HttpMethod.GET,"/api/stats").blockingHandler(new Stats());
		
		System.out.println("Configured paths:");
		for (Route r: router.getRoutes()) {
			
			System.out.println(r.getPath());
		}
		future.complete(router);
		
		return future;
	}
	
	private Future<Void> startServer(Router router) {
		
		System.out.println("** start server **");
		
		Future<Void> future = Future.future();
		
		server = vertx.createHttpServer()
			.requestHandler(router::accept)
			.listen(port, host, res -> {
				if (res.succeeded()) {
					future.complete();
				} else {
					future.fail(res.cause());
				}
			});
		
		return future;
	}
}
