package api.server;

import api.endpoints.News;
import api.endpoints.Stats;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public class Server extends AbstractVerticle {

	private HttpServer server;

	public void start(Future<Void> startFuture) {

		Router router = Router.router(vertx)
				.mountSubRouter("/api", new News(vertx).route())
				.mountSubRouter("/api", new Stats(vertx).route());

		server = vertx.createHttpServer()
				.requestHandler(router::accept).listen(8080, res -> {
					if (res.succeeded()) {
						startFuture.complete();
					} else {
						startFuture.fail(res.cause());
					}
				});
	}
}
