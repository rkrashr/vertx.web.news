package api.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.joda.time.DateTime;
import org.joda.time.Period;

import api.article.Article;
import api.endpoints.Stats;
import api.fetch.FetcherRSS;
import api.parse.ParserCnn;
import api.parse.ParserRSS;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.TimeoutStream;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.oauth2.AccessToken;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Observable;
import rx.Single;




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
		.flatMap(f -> this.initAuth())
		.flatMap(f -> this.auth(accessToken))
		.flatMap(f -> this.makeRoutes())
		.flatMap(router -> this.startServer(router))
		.subscribe(server -> {
			
			this.startFetcher(interval).flatMap(article -> article).subscribe(article -> {
				System.out.println(article);
			}, error -> {
				System.out.println(error);
			}, ()-> {
				System.out.println("** finished **");
			});
			
			startFuture.complete();
		
		});
		
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



	private Single<Void> configure() {
		
		ConfigStoreOptions fileStore = new ConfigStoreOptions()
				.setType("file")
				.setConfig(new JsonObject().put("path", "vertx.app.json"));
		
		ConfigRetrieverOptions options = 
				new ConfigRetrieverOptions()
				.addStore(fileStore);

		ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

		return retriever.rxGetConfig().map(config -> {
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

				}
				

				System.out.println("** configured **");
			    return null;
			});
	}
	
	private Single<Void> initAuth() {

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
		return Single.just(null);
	}
	
	private Single<JsonObject> auth(String accessToken) {

		JsonObject tokenConfig = new JsonObject().put("code", accessToken);

		System.out.println(tokenConfig);
		
		return oauth2.rxGetToken(tokenConfig).map(token -> {
			System.out.println(token.principal());
			return token.principal();
		});  
	}

	private Single<Router> makeRoutes() {
		System.out.println("** configure routes **");
		
		Router router = Router.router(vertx);
		
		router.route(HttpMethod.GET,"/api/stats").blockingHandler(new Stats());
		
		System.out.println("Configured paths:");
		for (Route r: router.getRoutes()) {
			
			System.out.println(r.getPath());
		}
		return Single.just(router);
	}
	
	private Single<HttpServer> startServer(Router router) {
	
		System.out.println("** starting server **");

		server = vertx.createHttpServer();
		
		return server
				.requestHandler(router::accept)
				.rxListen(port, host)
				.doOnSuccess(server -> System.out.println("** server started"));
	}
	
	private Observable<Observable<Article>> startFetcher(Period period) {

		System.out.println("** start fetcher **");

		WebClientOptions options = 
				new WebClientOptions()
				.setFollowRedirects(true)
				.setUserAgentEnabled(true)
				.setUserAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
		
		WebClient client = WebClient.create(vertx, options);
		
		final FetcherRSS fetcher = new FetcherRSS(vertx, url, DateTime.now().minusDays(5));
		final ParserRSS rssParser = new ParserRSS(client);
		final ParserCnn articleParser = new ParserCnn(vertx);

		TimeoutStream stream = vertx
			.periodicStream(interval.toDurationFrom(DateTime.now()).getMillis());

		return stream.toObservable()
		.flatMap(timestamp -> fetcher.next())
		.map(
			entryset->entryset
			.flatMap(entry -> rssParser.parse(entry).toObservable())
			.flatMap(a -> articleParser.parse(a))
		)
		.doOnTerminate(() -> System.out.println("** fetcher terminated **"));
	}
	
	
	
}
