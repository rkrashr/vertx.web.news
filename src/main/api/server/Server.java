package api.server;

import api.endpoints.News;
import api.endpoints.Stats;
import api.endpoints.Template;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class Server extends AbstractVerticle {

	private HttpServer server;

	public void start(Future<Void> startFuture) {

		Router router = Router.router(vertx);
		
		router.route(HttpMethod.GET,"/api/news").blockingHandler(new News());
		router.route(HttpMethod.GET,"/api/stats").blockingHandler(new Stats());
		router.route(HttpMethod.GET,"/api/tmp/*").blockingHandler(new Template(vertx));
		
		
		System.out.println("Configured paths:");
		for (Route r: router.getRoutes()) {
			
			System.out.println(r.getPath());
		}
		
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
