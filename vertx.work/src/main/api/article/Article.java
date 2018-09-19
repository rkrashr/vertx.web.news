package api.article;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.rometools.rome.feed.synd.SyndEntry;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public class Article {

	public String title;
	public String url;
	public List<String> authors;
	public String author;
	public String text;
	public String summary;
	public String headline;
	public String source;
	

	public Article(SyndEntry article) {
		this.title = article.getTitle();
		this.url = article.getLink();
		this.author = article.getAuthor();
		this.authors = article.getAuthors()
				.stream()
				.map(author -> author.getName())
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this,ToStringStyle.JSON_STYLE)
			.append("title", title)
			.append("headline", headline)
			.append("summary", summary)
			.append("url", url)
			.append("source_size", source.length())
			.append("author", author)
			.build();
	}

	public Future<Article> fetch(Vertx vertx) {

		Future<Article> future = Future.future();

		final Article a = this;
		HttpClientRequest req = vertx.createHttpClient().getAbs(url, new Handler<HttpClientResponse>() {
			public void handle(HttpClientResponse response) {
				response.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer data) {
						a.source = data.toString(Charset.defaultCharset());
						future.complete(a);
					}
				});
			}
		});
		req.end();

		return future;
	}

}
