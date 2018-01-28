import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class KHInsiderDownloader {

    private static final String[] FORMATS = {"mp3"}; // File type(s) to look for
    private static final String TITLE_SEPARATOR = ". "; // Track index and title separator, e.g. 1. Title
    private static final String BASE_URL = "https://downloads.khinsider.com/game-soundtracks/album/";

    // Configure the progressbar
    private static final char[] PBAR_ENDS = {'[', ']'};
    private static final char PBAR = '=';
    private static final char PBAR_EMPTY = ' ';
    private static final char PBAR_ARROW = '>';
    private static final int PBAR_LENGTH = 20; // The length of the progressbar, excluding the ends
    private static final String END_TEXT = "100%"; // Will be printed after the progressbar when finished
    // e.g. [=====>   ] 60%

    private static final String ALBUM_START = ":: "; // Gets printed before the album title, e.g. -- Album or :: Album

    public static void main(String[] args) throws Exception {
 
	// Cycle through the albums
        int u = 0;
        for (String albumURL : args) {
            // Add the full URL to the argument if necessary
            if (!albumURL.contains(BASE_URL)) {
                albumURL = BASE_URL + albumURL;
            }  

            // Store any failed downloads in a string
            String failedTracks = "";

            // Get the page html and select links found on the page
            Document doc = Jsoup.connect(albumURL).get();
            Elements pageLinks = doc.select("a");

            // Get the correct page links and track names and store them in lists
            List<String> albumTracks = new ArrayList<>();
            List<String> trackNames = new ArrayList<>();
            for (Element link : pageLinks) {
                String absHref = link.attr("abs:href");
                // Ignore any duplicates while adding the correct links to the list
                if (containsFormat(absHref) && !albumTracks.contains(absHref)) {
                    albumTracks.add(absHref);
                    trackNames.add(link.text());
                }
            }

            // Get the album size
            String albumSize = "";
            for (Element b : doc.select("b")) {
                if (Character.isDigit(b.text().charAt(0))
                        && Character.isLetter(b.text().charAt(b.text().length() - 1))) {
                    albumSize = b.text();
                    break;
                }
            }

            // Get the album title
            String albumTitle = doc.select("h2").first().text();
            System.out.println(ALBUM_START + albumTitle /* + " (" + albumSize + ")"*/);

            // Get file links for the tracks and download them
            List<String> trackURLs = new ArrayList<>();
            int i = 0;
            int m = trackNames.size();
            for (String albumTrack : albumTracks) {
                // Print out the progressbar
                printProgress(i, m);

                // Download the track
                trackURLs.add(getLinks(albumTrack).get(0));
                try {
                    FileUtils.copyURLToFile(
                            new URL(trackURLs.get(i)),
                            new File(albumTitle + "/" + (i + 1) + TITLE_SEPARATOR + trackNames.get(i) + "." + FORMATS[0])
                    );
                } catch (Exception e) {
                    failedTracks += "'" + trackNames.get(i) + "' from " + trackURLs.get(i) + "\n";
                }
                i++;
            }
            printProgress(m, m);

            // Check if some tracks couldn't be downloaded
            if (!failedTracks.equals("")) {
                System.out.print("** Couldn't download the following tracks\n" + failedTracks);
            } else {
                System.out.println("\n");
            }

            u++;
        }
    }

    // Get links for the files from a track's page
    private static List<String> getLinks(String URL) throws IOException {
        // Get the page html and select links found on the page
        Document doc = Jsoup.connect(URL).get();
        Elements pageLinks = doc.select("a");

        // Get the correct page links and store them in a list
        List<String> trackLinks = new ArrayList<>();
        for (Element link : pageLinks) {
            String absHref = link.attr("abs:href");
            // Ignore any duplicates while adding the correct links to the list
            if (containsFormat(absHref) && !trackLinks.contains(absHref)) {
                trackLinks.add(absHref);
            }
        }

        return trackLinks;
    }

    // Check if a string (in this case a link) contains any downloadable formats
    private static boolean containsFormat(String input) {
        return Arrays.stream(FORMATS).parallel().anyMatch(input::contains);
    }

    // Repeat characters x times
    private static String repeat(char input, int repeat) {
        String r = "";
        for (int i = 0; i < repeat; i++) {
            r += input;
        }
        return r;
    }

    // Print the current progress
    private static void printProgress(int i, int max) {
        String pbar;
        float p = (float)i / (float)max; // Current progress, eg. 0.1f or 1.0f
        int progress = (int)(p * PBAR_LENGTH);
        if (i != max) {
            pbar = PBAR_ENDS[0]
                    + repeat(PBAR, (int)(progress))
                    + PBAR_ARROW
                    + repeat(PBAR_EMPTY, PBAR_LENGTH - progress - 1)
                    + PBAR_ENDS[1]
                    + " " + (int)((float)p * 100) + "%"
                    + " (" + i + "/" + max + ")";
        } else {
            pbar = PBAR_ENDS[0]
                    + repeat(PBAR, progress)
                    + PBAR_ENDS[1] + " " + END_TEXT
                    + " (" + i + "/" + max + ")";
        }

        System.out.print("\r" + pbar);
    }
}
