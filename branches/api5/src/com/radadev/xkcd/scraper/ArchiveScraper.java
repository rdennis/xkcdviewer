/*
 * Copyright (C) 2010  Alex Avance
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-
 * 1307, USA.
 */
package com.radadev.xkcd.scraper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.text.Html;

import com.radadev.xkcd.Comics;

public final class ArchiveScraper {

  public static final String COMIC_XPATH= "//a[@title]";

  protected static boolean mArchiveScraped= false;
  protected static SortedMap<Integer, String> mComicMap= new TreeMap<Integer, String>();

  protected static TagNode mNode;

  public synchronized static SortedMap<Integer, String> getComicList() throws IOException {
    if (!mArchiveScraped) {
      try {
        scrapeArchive();
      } catch (XPatherException e) {
        // something went wrong with the scraper, this isn't the client's responsibility
        // return null to signify error
        e.printStackTrace();
        return null;
      }
    }
    return mComicMap;
  }

  public synchronized static void scrapeArchive() throws IOException, XPatherException {

    HtmlCleaner cleaner= new HtmlCleaner();
    CleanerProperties properties= cleaner.getProperties();
    properties.setAllowHtmlInsideAttributes(true);
    properties.setOmitUnknownTags(true);
    properties.setAllowMultiWordAttributes(true);
    properties.setRecognizeUnicodeChars(true);
    properties.setOmitComments(true);

    URL url= new URL(Comics.URL_ARCHIVE);
    URLConnection conn= url.openConnection();
    mNode= cleaner.clean(new InputStreamReader(conn.getInputStream()));

    Object[] comicNodes= mNode.evaluateXPath(COMIC_XPATH);

    for (Object nodeObject : comicNodes) {
      TagNode node= (TagNode) nodeObject;
      if (node.hasAttribute("href")) {
        try {
          String title= Html.fromHtml(node.getText().toString()).toString();
          Integer number= Integer.parseInt(node.getAttributeByName("href").replaceAll("/", ""));
          mComicMap.put(number, title);
        } catch (NumberFormatException e) {
          // number wasn't found correctly, just ignore it
        }
      }
    }

    mArchiveScraped= true;
  }
}
