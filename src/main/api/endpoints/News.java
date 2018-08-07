package api.endpoints;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class News implements Endpoint {

	private final Vertx vertx;
	
	public News(Vertx vertx) {
		this.vertx = vertx;
	}
	
	@Override
	public Router route() {
		
		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/news").blockingHandler(this::getNews);

		return router;
	}


	private void getNews(RoutingContext context) {
		context.request().response().end("Handled News");
	}
	
	
	
}
