package api.parse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.rometools.rome.feed.synd.SyndEntry;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.rxjava.core.Future;

public class Article {

	String title;
	String body;
	String url;
	List<String> authors;

	public Article(SyndEntry article) {
		this.title = article.getTitle();
		this.url = article.getLink();
		this.body = "";
	}

	@Override
	public String toString() {
		return title;
	}

	public Future<Void> fetch(Vertx vertx) {

		Future<Void> future = Future.future();

		try {
			URL _url = new URL(url);

			HttpClientRequest req = vertx.createHttpClient().get(url, new Handler<HttpClientResponse>() {
				public void handle(HttpClientResponse response) {
					response.bodyHandler(new Handler<Buffer>() {
						public void handle(Buffer data) {
							System.out.println("Got response data:" + data);
						}
					});
				}
			});
			
			req.end();
			
		} catch (MalformedURLException e) {
			future.fail(e);
		}

		return future;
	}

}
