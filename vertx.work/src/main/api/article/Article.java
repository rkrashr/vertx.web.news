package api.article;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
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
	public List<String> categories;
	public List<String> keywords;
	public Date published;
	public Date updated;
	

	public Article(SyndEntry article) {
		this.title = article.getTitle();
		this.url = article.getLink();
		this.author = article.getAuthor();
		this.authors = article.getAuthors()
				.stream()
				.map(author -> author.getName())
				.collect(Collectors.toList());
		this.categories = article.getCategories().stream().map(c -> c.getName()).collect(Collectors.toList());
		this.keywords = Collections.emptyList();
		this.published = article.getPublishedDate();
		this.updated = article.getUpdatedDate();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this,ToStringStyle.JSON_STYLE)
			.append("title", title)
			.append("headline", headline)
			.append("summary", summary)
			.append("categories", categories)
			.append("keywords", keywords)
			.append("url", url)
			.append("source_size", source.length())
			.append("author", author)
			.append("published", published)
			.append("updated", updated)
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
