package be.tgc.htmlsnapshot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rajeevguru on 12/07/14.
 */
public class Snapper {

    private WebClient webClient;

    private final Pattern pattern = Pattern.compile("<!--\\s*htmlsnapshot\\s+pagename\\s*=\\s*\"(\\w+)\\.html\"\\s*-->");

    private final Pattern filenameChecker = Pattern.compile("(\\w+)(\\d+)\\.html");

    private Set<String> crawledUrls = new HashSet<String>();


    private String baseUrl;


    public Snapper() {

        webClient = new WebClient(BrowserVersion.CHROME);

        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

    }

    public static void main(String[] params) {


        try {

            Snapper snapper = new Snapper();
            String base = params[0];
            snapper.setBaseUrl(base);
            if (params.length >1 ){
                snapper.startCrawl(params[1]);
            }
            else {
                snapper.startCrawl("");
            }



        } catch (Exception e) {
            e.printStackTrace();
            //todo deal with exception
        }

    }


    private HtmlPage getPage(String url) throws IOException {

        WebClient webClient1 = new WebClient(BrowserVersion.CHROME);

        webClient1.setAjaxController(new NicelyResynchronizingAjaxController());

        HtmlPage page = webClient1.getPage(getBaseUrl() + url);
        webClient1.waitForBackgroundJavaScriptStartingBefore(1000);
        return page;
    }


    private void savePageToDisk(HtmlPage page) throws IOException {

        // look for the file name in the page
        final String pageAsXml = page.asXml();

        String fileName = "default.html";
        Matcher matcher = pattern.matcher(pageAsXml);

        if (matcher.find()) {

            fileName = matcher.group(1) + ".html";

        }

        File f = new File(fileName);
        while (f.exists()) {

            String name;
            Integer i;
            Matcher matcher1 = filenameChecker.matcher(fileName);
            if (matcher1.find()) {

                name = matcher1.group(1);
                i = Integer.parseInt(matcher1.group(2));

            } else {
                name = fileName.substring(0, fileName.length() - 5);
                i = 0;
            }

            fileName = name + ++i + ".html";
            f = new File(fileName);
        }

        Files.write(Paths.get(fileName), pageAsXml.getBytes("UTF-8"));

    }


    private List<String> getLinksOnPage(HtmlPage page) {

        List<String> validLinks = new ArrayList<String>();
        List<HtmlAnchor> anchorList = page.getAnchors();

        for (Iterator iter = anchorList.iterator();
             iter.hasNext(); ) {
            HtmlAnchor anchor = (HtmlAnchor) iter.next();
            String hrefAttribute = anchor.getHrefAttribute();
            if (hrefAttribute.startsWith("#!")) {
                validLinks.add(hrefAttribute);
            }
        }

        return validLinks;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // recursive method to get all the valid link on the page
    private void startCrawl(String url) throws IOException {

        System.out.println("Crawling url:" + url);

        if (!crawledUrls.add(url)) {
            return;
        }

        HtmlPage page = this.getPage(url);



        this.savePageToDisk(page);

        List<String> validUrls = this.getLinksOnPage(page);

        for (String uri: validUrls) {
            startCrawl(uri);
        }


    }

}
