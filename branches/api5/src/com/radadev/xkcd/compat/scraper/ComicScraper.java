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
package com.radadev.xkcd.compat.scraper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.text.Html;
import android.util.Pair;

import com.radadev.xkcd.compat.Comics;

public final class ComicScraper {

  public static final String COMIC_XPATH= "//img[@id='comic']";
  
  /**
   * Download and extract comic information for a given comic.
   * 
   * @param number the number of the comic to check
   * @return a pair containing the image url, and the hover text
   * @throws IOException if the connection fails
   */
  public static Pair<String, String> getComicInformation(int number) throws IOException {
    HtmlCleaner cleaner= new HtmlCleaner();
    CleanerProperties properties= cleaner.getProperties();
    properties.setAllowHtmlInsideAttributes(true);
    properties.setAllowMultiWordAttributes(true);
    properties.setRecognizeUnicodeChars(true);
    properties.setOmitComments(true);

    TagNode imageNode;
    
    try {
      URL url= new URL(Comics.URL_MAIN + number + "/index.html");
      URLConnection conn= url.openConnection();

      TagNode node= cleaner.clean(new InputStreamReader(conn.getInputStream()));

      imageNode= (TagNode) node.evaluateXPath(COMIC_XPATH)[0];
    } catch (XPatherException e) {
      // something went wrong with the scraper, this isn't the client's responsibility
      // return null to signify error
      e.printStackTrace();
      return null;
    }

    String src= imageNode.getAttributeByName("src");
    String text= Html.fromHtml(Html.fromHtml(imageNode.getAttributeByName("title")).toString()).toString();
  
    return new Pair<String, String>(src, text);
  }
}
