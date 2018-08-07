package api.endpoints;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class Stats implements Endpoint {

	private final Vertx vertx;
	
	public Stats(Vertx vertx) {
		this.vertx = vertx;
	}
	
	@Override
	public Router route() {
		
		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/stats").blockingHandler(this::getStats);

		return router;
	}


	private void getStats(RoutingContext context) {
		context.request().response().end("Handled Stats");
	}
	
	
	
}
