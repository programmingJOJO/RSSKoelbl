package mi.rssKoelbl;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

class RSSParser extends DefaultHandler {

    private static final String TAG_TITLE = "title";

    // Number of articles to download
	private static final int ARTICLES_LIMIT = 10;
	
	// Helper method
	private String getNodeValueByTagName(Node parentNode, String tagNameOfNode)
	{
	    String nodeValue = "";
	    if (((Element) parentNode).getElementsByTagName(tagNameOfNode).getLength() != 0)
	        if (((Element) parentNode).getElementsByTagName(tagNameOfNode).item(0).hasChildNodes())
	        {
	            nodeValue = ((Element) parentNode).getElementsByTagName(tagNameOfNode).item(0).getChildNodes().item(0).getNodeValue();
	        }
	    return nodeValue;
	}
	
	public Feed getFeed(String rss_url) {
		Feed feed = null;
		String feed_xml;
		
		if (rss_url != null) {
			
			feed_xml = getXmlFromUrl(rss_url);
			
            // Check if RSS XML fetched or not
            if (feed_xml != null) {
                try {
                	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(new InputSource(new URL(rss_url).openStream()));
                    doc.getDocumentElement().normalize();

                    String TAG_CHANNEL = "channel";
                    NodeList nl = doc.getElementsByTagName(TAG_CHANNEL);
                    
					 // Looping through all item nodes <item>     
					 for (int i = 0; i < nl.getLength(); i++) {
						 Node node = nl.item(i);
					     String title = getNodeValueByTagName(node, TAG_TITLE);
					     					     
					     // Creating new RSS Feed
					     feed = new Feed();
					     feed.title = title;
					     feed.rss_url = rss_url;
					     feed.notify = 1; // Default
					     feed.widgetable = 0;
					 }
                } catch (Exception e) {
                    // Check log for errors
                    e.printStackTrace();
                }
            } else {
                // Failed to fetch rss xml
            	Log.e("RSSPARSER", "Failed to fetch rss xml");
            }
        } else {
            // No rss url found
        	Log.e("RSSPARSER", "No rss url found");
        }
        return feed;
	}
	
	
	private String getXmlFromUrl(String url) {
        String xml = null;
 
        try {
            // Request method is GET
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
 
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            xml = EntityUtils.toString(httpEntity);
 
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // return XML
        return xml;
    }
	
    public Document getDomElement(String xml) {
        Document doc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
 
            DocumentBuilder db = dbf.newDocumentBuilder();
 
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);
 
        } catch (ParserConfigurationException e) {
            Log.e("Error: ", e.getMessage());
            return null;
        } catch (SAXException e) {
            Log.e("Error: ", e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e("Error: ", e.getMessage());
            return null;
        }
 
        return doc;
    }
    
    public String getValue(Element item, String str) {     
        NodeList n = item.getElementsByTagName(str);
        return this.getElementValue(n.item(0));
    }
     
    private String getElementValue(Node elem) {
             Node child;
             if( elem != null){
                 if (elem.hasChildNodes()){
                     for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
                         if( child.getNodeType() == Node.TEXT_NODE  ){
                             return child.getNodeValue();
                         }
                     }
                 }
             }
             return "";
      } 
    
    public List<Article> getArticles(String rss_url){
        List<Article> articleList = new ArrayList<Article>();
        String rss_feed_xml;
        
        // Get rss xml from rss url
        rss_feed_xml = getXmlFromUrl(rss_url);
        // Check if rss xml fetched or not
        if(rss_feed_xml != null){
            try {
            	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new InputSource(new URL(rss_url).openStream()));
                doc.getDocumentElement().normalize();
                 
                // Getting items array
                String TAG_ITEM = "item";
                NodeList items = doc.getElementsByTagName(TAG_ITEM);
                 
                // looping through each item
                for(int i = 0; i < ARTICLES_LIMIT; i++) {
                	Node node = items.item(i);
                	String title = getNodeValueByTagName(node, TAG_TITLE);
                    String TAG_LINK = "link";
                    String link = getNodeValueByTagName(node, TAG_LINK);
                    String TAG_DATE = "pubDate";
                    String published_at = getNodeValueByTagName(node, TAG_DATE).length() > 0 ? getNodeValueByTagName(node, TAG_DATE) : "Datum nicht vorhanden";
                    String TAG_GUID = "guid";
                    String guid = getNodeValueByTagName(node, TAG_GUID).length() > 0 ? getNodeValueByTagName(node, TAG_GUID) : title + link;
                    
                    Article article = new Article();
                    article.title = title;
                    article.url = link;
                    article.published_at = published_at;
                    article.guid = guid;
                    // Adding item to list
                    articleList.add(article);
                }
            }catch(Exception e){
                // Check log for errors
                e.printStackTrace();
            }
        }
         
        // Return item list
        return articleList;
    }
}
