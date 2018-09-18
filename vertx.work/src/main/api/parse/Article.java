package api.parse;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndPerson;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public class Article {

	String title;
	String body;
	String url;
	List<SyndPerson> authors;
	String author;
	Document document;

	public Article(SyndEntry article) {
		this.title = article.getTitle();
		this.url = article.getLink();
		this.author = article.getAuthor();
		this.authors = article.getAuthors();
		this.body = "";
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this,ToStringStyle.JSON_STYLE)
			.append("title", title)
			.append("url", url)
			.append("body_size", body.length())
			.append("document", document.body().childNodeSize())
			.append("author", author)
			.append("authors", authors)
			.build();
	}

	public Future<Article> fetch(Vertx vertx) {

		Future<Article> future = Future.future();

		final Article a = this;
		HttpClientRequest req = vertx.createHttpClient().getAbs(url, new Handler<HttpClientResponse>() {
			public void handle(HttpClientResponse response) {
				response.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer data) {
						a.body = data.toString(Charset.defaultCharset());
						a.document = Jsoup.parse(a.body);
						future.complete(a);
					}
				});
			}
		});
		req.end();

		return future;
	}

}
