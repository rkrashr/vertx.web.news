package api.parse;

import com.rometools.rome.feed.synd.SyndEntry;

public class Parser {

	public Parser() {
	}
	
	public Article parse(SyndEntry entry)  {
		
			return new Article(entry.getTitle());
	}
	
}
