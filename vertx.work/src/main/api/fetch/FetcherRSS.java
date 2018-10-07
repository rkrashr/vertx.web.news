package api.fetch;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.joda.time.DateTime;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.circuitbreaker.CircuitBreaker;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

public class FetcherRSS {

	
	private final URL url;
	private Date since;
	
	private final Vertx vertx;
	private final CircuitBreaker breaker;
	
	public FetcherRSS(Vertx vertx, URL url, DateTime since) {
		this.vertx = vertx;
		this.url = url;
		this.since = since.toDate();
		
        this.breaker = CircuitBreaker.create("rss-circuit-breaker", vertx,
                new CircuitBreakerOptions().setMaxFailures(5).setMaxRetries(2)); 
	}
	
	public ObservableFuture<Observable<SyndEntry>> next() {
		System.out.println("туче");
		final ObservableFuture<Observable<SyndEntry>> future = RxHelper.observableFuture();
		vertx.executeBlocking(blocking -> {

			System.out.println(".");
			SyndFeedInput input = new SyndFeedInput();
			try {
				
				
				breaker.<Stream<SyndEntry>>execute(exe -> {
					
					
					SyndFeed feed;
					try {
						feed = input.build(new XmlReader(url));

						
						System.out.println(url);
						List<SyndEntry> entries = feed.getEntries();
						
						Date limit = Date.from(since.toInstant());
						System.out.println(limit);
						System.out.println(entries.size());
						Stream<SyndEntry> filtered = feed.getEntries().stream().filter(entry -> entry.getPublishedDate().after(limit));
	
						this.since = entries
								.stream()
								.map(entry -> entry.getPublishedDate())
								.max((d1,d2) -> d1.compareTo(d2))
								.orElse(this.since);
						
						exe.complete(filtered);
						
					} catch (IllegalArgumentException | FeedException | IOException e) {
						e.printStackTrace();
						exe.fail(e);
					}
					
		        }).setHandler(res -> {
		        	if (res.succeeded())
		        		blocking.complete(ObservableFuture.from(res.result()::iterator));
		        	else
		        		blocking.fail(res.cause());
		        });
		        
				


				
				
			} catch (Exception e) {
				e.printStackTrace();
				blocking.fail(e);
			}
		}, future.toHandler());
		
		return future;
	}
	
}
