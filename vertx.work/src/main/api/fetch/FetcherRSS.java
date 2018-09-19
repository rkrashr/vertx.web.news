package api.fetch;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.joda.time.DateTime;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import rx.Observable;

public class FetcherRSS {

	URL url;
	Date since;
	
	public FetcherRSS(URL url, DateTime since) {
		this.url = url;
		this.since = since.toDate();
	}
	
	public Observable<SyndEntry> next() {
		System.out.println(".");
		SyndFeedInput input = new SyndFeedInput();
		try {
			SyndFeed feed = input.build(new XmlReader(url));
			List<SyndEntry> entries = feed.getEntries();
			
			Date limit = Date.from(since.toInstant());
			System.out.println(limit);
			Stream<SyndEntry> filtered = feed.getEntries().stream().filter(entry -> entry.getPublishedDate().after(limit));

			this.since = entries
					.stream()
					.map(entry -> entry.getPublishedDate())
					.max((d1,d2) -> d1.compareTo(d2))
					.orElse(this.since);

			return Observable.from(filtered::iterator);
			
		} catch (Exception e) {
			e.printStackTrace();
			return Observable.empty();
		}
		
	}
	
}
