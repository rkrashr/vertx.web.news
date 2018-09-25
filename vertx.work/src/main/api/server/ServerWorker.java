package api.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.joda.time.DateTime;
import org.joda.time.Period;

import api.article.Article;
import api.endpoints.Stats;
import api.fetch.FetcherRSS;
import api.parse.ParserCBC;
import api.parse.ParserRSS;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.TimeoutStream;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.AzureADAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.rx.java.RxHelper;


public class ServerWorker extends AbstractVerticle {

	private static String DEFAULT_HOST = "0.0.0.0";
	private static int DEFAULT_PORT = 8080;
	private static String DEFAULT_INTERVAL = "PT10S";
	private static String DEFAULT_URL = "";

	private String host;
	private int port;
	private Period interval;
	private URL url;
	
	private OAuth2Auth oauth2;

	private String clientId;
	private String clientSecret;
	private String tenantId;
	
	private String accessToken;

	private HttpServer server;

	public void start(Future<Void> startFuture) {

		configure()
		.compose(f -> this.initAuth())
		.compose(f -> this.auth(accessToken))
		.compose(f -> this.makeRoutes())
		.compose(router -> this.startServer(router))
		.compose(f -> this.startFetcher(interval))
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
			    interval = Period.parse(config.getString("interval", DEFAULT_INTERVAL));

			    tenantId = config.getString("tenant-id");
			    clientId = config.getString("client-id");
			    clientSecret = config.getString("client-secret");

			    accessToken = config.getString("access-token");

			    try {
					url = new URL(config.getString("url",DEFAULT_URL));
				} catch (MalformedURLException e) {
					future.fail(new IOException("Bad url", e));
				}
			    System.out.println("** configured **");
			    future.complete();
			}
		});

		return future;
	}
	
	private Future<Void> initAuth() {

		oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.CLIENT, new OAuth2ClientOptions(new HttpClientOptions())
		        .setSite("https://login.windows.net/" + tenantId)
		        .setTokenPath("/oauth2/token")
		        .setAuthorizationPath("/oauth2/authorize")
		        .setScopeSeparator(",")
		        .setClientID(clientId)
		        .setClientSecret(clientSecret)
		        .setExtraParameters(
		          new JsonObject().put("resource", "https://management.core.windows.net/")));
		
		
		
		System.out.println("Azure authenticator created");
		return Future.succeededFuture();
	}
	
	private Future<User> auth(String accessToken) {

		Future<User> future = Future.future();
		
		JsonObject tokenConfig = new JsonObject()
			    .put("code", accessToken);

		oauth2.authenticate(tokenConfig, res -> {
			System.out.println(tokenConfig);
			if (res.failed()) {
				System.err.println("Access Token Error: " + res.cause().getMessage());
				future.fail(res.cause().getMessage());
			} else {
				User token = res.result();
				future.complete(token);
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
	
	private Future<Void> startFetcher(Period period) {

		System.out.println("** start fetcher **");
		
		Future<Void> future = Future.future();

		FetcherRSS fetcher = new FetcherRSS(url, DateTime.now().minusDays(5));
		ParserRSS rssParser = new ParserRSS(vertx);
		ParserCBC cbcParser = new ParserCBC(vertx);
		
		TimeoutStream stream = vertx
			.periodicStream(interval.toDurationFrom(DateTime.now()).getMillis());

		RxHelper.toObservable(stream)
		.flatMap(timestamp -> fetcher.next())
		.map(entry -> rssParser.parse(entry))
		.map(article -> cbcParser.parse(article))
		.subscribe(
				article -> article.setHandler(new Handler<AsyncResult<Article>>() {

					@Override
					public void handle(AsyncResult<Article> event) {

						if (event.succeeded()) {
							Article a = event.result();
							System.out.println(a);
						}
						else
							event.cause().printStackTrace();
					}
					
				}), 
				ex -> System.out.println(ex), 
				() -> System.out.println("done"));
				
		future.complete();
		return future;
	}
	
	
	
}
