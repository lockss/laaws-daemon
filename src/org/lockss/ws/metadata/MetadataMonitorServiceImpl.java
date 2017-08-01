/*

 Copyright (c) 2015-2017 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.ws.metadata;

import static org.lockss.metadata.SqlConstants.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jws.WebService;
import org.lockss.app.LockssApp;
import org.lockss.app.LockssDaemon;
import org.lockss.metadata.Isbn;
import org.lockss.metadata.Issn;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.AuMetadataWsResult;
import org.lockss.ws.entities.KeyIdNamePairListPair;
import org.lockss.ws.entities.KeyValueListPair;
import org.lockss.ws.entities.IdNamePair;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.LockssWebServicesFaultInfo;
import org.lockss.ws.entities.MetadataItemWsResult;
import org.lockss.ws.entities.MismatchedMetadataChildWsResult;
import org.lockss.ws.entities.PkNamePair;
import org.lockss.ws.entities.PkNamePairIdNamePairListPair;
import org.lockss.ws.entities.UnnamedItemWsResult;

/**
 * The MetadataMonitor web service implementation.
 */
@WebService
public class MetadataMonitorServiceImpl implements MetadataMonitorService {
  private static Logger log =
      Logger.getLogger(MetadataMonitorServiceImpl.class);

  /**
   * Provides the names of the publishers in the database.
   * 
   * @return a List<String> with the publisher names.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<String> getPublisherNames() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublisherNames(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<String> results =
	  (List<String>)(getMetadataManager().getPublisherNames());

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "results.size() = " + results.size());
      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the DOI prefixes for the publishers in the database with multiple
   * DOI prefixes.
   * 
   * @return a List<KeyValueListPair> with the DOI prefixes keyed by the
   *         publisher name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getPublishersWithMultipleDoiPrefixes()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublishersWithMultipleDoiPrefixes(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the DOI prefixes linked to the publishers.
      Map<String, Collection<String>> publishersDoiPrefixes =
	  getMetadataManager().getPublishersWithMultipleDoiPrefixes();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "publishersDoiPrefixes.size() = " + publishersDoiPrefixes.size());

      // Check whether there are results to display.
      if (publishersDoiPrefixes.size() > 0) {
        // Yes: Loop through the publishers.
        for (String publisherName : publishersDoiPrefixes.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : publishersDoiPrefixes.get(publisherName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(publisherName, prefixes));
        }
      }

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "results.size() = " + results.size());
      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publisher names linked to DOI prefixes in the database that
   * are linked to multiple publishers.
   * 
   * @return a List<KeyValueListPair> with the publisher names keyed by the DOI
   *         prefixes to which they are linked.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getDoiPrefixesWithMultiplePublishers()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getDoiPrefixesWithMultiplePublishers(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publishers linked to the DOI prefixes.
      Map<String, Collection<String>> doiPrefixesPublishers =
	  getMetadataManager().getDoiPrefixesWithMultiplePublishers();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "doiPrefixesPublishers.size() = " + doiPrefixesPublishers.size());

      // Check whether there are results to display.
      if (doiPrefixesPublishers.size() > 0) {
        // Yes: Loop through the prefixes.
        for (String prefix : doiPrefixesPublishers.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "prefix = " + prefix);

          ArrayList<String> publisherNames = new ArrayList<String>();

          for (String publisherName : doiPrefixesPublishers.get(prefix)) {
            if (log.isDebug3())
              log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);
            publisherNames.add(publisherName);
          }

          results.add(new KeyValueListPair(prefix, publisherNames));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the DOI prefixes for the Archival Units in the database with
   * multiple DOI prefixes.
   * 
   * @return a List<KeyValueListPair> with the DOI prefixes keyed by the
   *         Archival Unit identifier.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getAuIdsWithMultipleDoiPrefixes()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuIdsWithMultipleDoiPrefixes(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the DOI prefixes linked to the Archival Units.
      Map<String, Collection<String>> ausDoiPrefixes =
	  getMetadataManager().getAuIdsWithMultipleDoiPrefixes();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "ausDoiPrefixes.size() = " + ausDoiPrefixes.size());

      // Check whether there are results to display.
      if (ausDoiPrefixes.size() > 0) {
        // Yes: Loop through the Archival Unit identifiers.
        for (String auId : ausDoiPrefixes.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : ausDoiPrefixes.get(auId)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(auId, prefixes));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the DOI prefixes for the Archival Units in the database with
   * multiple DOI prefixes.
   * 
   * @return a List<KeyValueListPair> with the DOI prefixes keyed by the
   *         Archival Unit name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getAuNamesWithMultipleDoiPrefixes()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuNamesWithMultipleDoiPrefixes(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the DOI prefixes linked to the Archival Units.
      Map<String, Collection<String>> ausDoiPrefixes =
	  getMetadataManager().getAuNamesWithMultipleDoiPrefixes();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "ausDoiPrefixes.size() = " + ausDoiPrefixes.size());

      // Check whether there are results to display.
      if (ausDoiPrefixes.size() > 0) {
        // Yes: Loop through the Archival Unit names.
        for (String auName : ausDoiPrefixes.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auName = " + auName);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : ausDoiPrefixes.get(auName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(auName, prefixes));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISBNs for the publications in the database with more than two
   * ISBNS.
   * 
   * @return a List<KeyIdNamePairListPair> with the ISBNs keyed by the
   *         publication name. The IdNamePair objects contain the ISBN as the
   *         identifier and the ISBN type as the name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyIdNamePairListPair> getPublicationsWithMoreThan2Isbns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublicationsWithMoreThan2Isbns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyIdNamePairListPair> results =
	  new ArrayList<KeyIdNamePairListPair>();

      Map<String, Collection<Isbn>> publicationsIsbns =
	  getMetadataManager().getPublicationsWithMoreThan2Isbns();

      for (String publication : publicationsIsbns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publication = " + publication);

	ArrayList<IdNamePair> isbnResults = new ArrayList<IdNamePair>();

	for (Isbn isbn : publicationsIsbns.get(publication)) {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);
	  isbnResults.add(new IdNamePair(isbn.getValue(), isbn.getType()));
	}

        results.add(new KeyIdNamePairListPair(publication, isbnResults));
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISSNs for the publications in the database with more than two
   * ISSNS.
   * 
   * @return a List<KeyIdNamePairListPair> with the ISSNs keyed by the
   *         publication name. The IdNamePair objects contain the ISSN as the
   *         identifier and the ISSN type as the name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyIdNamePairListPair> getPublicationsWithMoreThan2Issns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublicationsWithMoreThan2Issns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyIdNamePairListPair> results =
	  new ArrayList<KeyIdNamePairListPair>();

      Map<org.lockss.metadata.PkNamePair, Collection<Issn>> publicationsIssns =
	  getMetadataManager().getPublicationsWithMoreThan2Issns();

      for (org.lockss.metadata.PkNamePair publication
	  : publicationsIssns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publication = " + publication);

	ArrayList<IdNamePair> issnResults = new ArrayList<IdNamePair>();

	for (Issn issn : publicationsIssns.get(publication)) {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);
	  issnResults.add(new IdNamePair(issn.getValue(), issn.getType()));
	}

        results.add(new KeyIdNamePairListPair(publication.getName(),
            issnResults));
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISSNs for the publications in the database with more than two
   * ISSNS.
   * 
   * @return a List<PkNamePairIdNamePairListPair> with the ISSNs keyed by the
   *         publication PK/name pair. The IdNamePair objects contain the ISSN
   *         as the identifier and the ISSN type as the name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<PkNamePairIdNamePairListPair>
  getIdPublicationsWithMoreThan2Issns() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublicationsWithMoreThan2Issns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<PkNamePairIdNamePairListPair> results =
	  new ArrayList<PkNamePairIdNamePairListPair>();

      Map<org.lockss.metadata.PkNamePair, Collection<Issn>> publicationsIssns =
	  getMetadataManager().getPublicationsWithMoreThan2Issns();

      for (org.lockss.metadata.PkNamePair publication
	  : publicationsIssns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publication = " + publication);

	ArrayList<IdNamePair> issnResults = new ArrayList<IdNamePair>();

	for (Issn issn : publicationsIssns.get(publication)) {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);
	  issnResults.add(new IdNamePair(issn.getValue(), issn.getType()));
	}

        results.add(new PkNamePairIdNamePairListPair(
            new PkNamePair(publication.getPk(), publication.getName()),
            issnResults));
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publication names linked to ISBNs in the database that are
   * linked to multiple publications.
   * 
   * @return a List<KeyValueListPair> with the publication names keyed by the
   *         ISBNs to which they are linked.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getIsbnsWithMultiplePublications()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getIsbnsWithMultiplePublications(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publications linked to the ISBNs.
      Map<String, Collection<String>> isbnsPublications =
	  getMetadataManager().getIsbnsWithMultiplePublications();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "isbnsPublications.size() = " + isbnsPublications.size());

      // Check whether there are results to display.
      if (isbnsPublications.size() > 0) {
        // Yes: Loop through the ISBNs. 
        for (String isbn : isbnsPublications.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : isbnsPublications.get(isbn)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(isbn, prefixes));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publication names linked to ISSNs in the database that are
   * linked to multiple publications.
   * 
   * @return a List<KeyValueListPair> with the publication names keyed by the
   *         ISSNs to which they are linked.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getIssnsWithMultiplePublications()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getIssnsWithMultiplePublications(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publications linked to the ISSNs.
      Map<String, Collection<String>> issnsPublications =
	  getMetadataManager().getIssnsWithMultiplePublications();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "issnsPublications.size() = " + issnsPublications.size());

      // Check whether there are results to display.
      if (issnsPublications.size() > 0) {
        // Yes: Loop through the ISSNs. 
        for (String issn : issnsPublications.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : issnsPublications.get(issn)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(issn, prefixes));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISSNs for books in the database.
   * 
   * @return a List<KeyValueListPair> with the ISSNs keyed by the publication
   *         name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getBooksWithIssns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getBooksWithIssns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the ISSNs linked to the books.
      Map<String, Collection<String>> booksWithIssns =
	  getMetadataManager().getBooksWithIssns();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "booksWithIssns.size() = " + booksWithIssns.size());

      // Check whether there are results to display.
      if (booksWithIssns.size() > 0) {
        // Yes: Loop through the books.
        for (String bookName : booksWithIssns.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "bookName = " + bookName);

          ArrayList<String> issns = new ArrayList<String>();

          for (String issn : booksWithIssns.get(bookName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);
            issns.add(issn);
          }

          results.add(new KeyValueListPair(bookName, issns));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISBNs for periodicals in the database.
   * 
   * @return a List<KeyValueListPair> with the ISBNs keyed by the publication
   *         name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getPeriodicalsWithIsbns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPeriodicalsWithIsbns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the ISBNs linked to the periodicals.
      Map<String, Collection<String>> periodicalsWithIsbns =
	  getMetadataManager().getPeriodicalsWithIsbns();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "periodicalsWithIsbns.size() = " + periodicalsWithIsbns.size());

      // Check whether there are results to display.
      if (periodicalsWithIsbns.size() > 0) {
        // Yes: Loop through the periodicals.
        for (String periodicalName : periodicalsWithIsbns.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "periodicalName = " + periodicalName);

          ArrayList<String> isbns = new ArrayList<String>();

          for (String isbn : periodicalsWithIsbns.get(periodicalName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);
            isbns.add(isbn);
          }

          results.add(new KeyValueListPair(periodicalName, isbns));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the Archival Units in the database with an unknown provider.
   * 
   * @return a List<String> with the sorted Archival Unit names.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<String> getUnknownProviderAuIds() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getUnknownProviderAuIds(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      return ((List<String>)(getMetadataManager().getUnknownProviderAuIds()));
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the journal articles in the database whose parent is not a
   * journal.
   * 
   * @return a List<MismatchedChildWsResult> with the mismatched journal
   *  articles sorted by Archival Unit, parent name and child name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<MismatchedMetadataChildWsResult>
  getMismatchedParentJournalArticles() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getMismatchedParentJournalArticles(): ";
    List<MismatchedMetadataChildWsResult> mismatchedChildren =
	new ArrayList<MismatchedMetadataChildWsResult>();

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      for (Map<String, String> mismatchedChild :
	getMetadataManager().getMismatchedParentJournalArticles()) {
	MismatchedMetadataChildWsResult result =
	    new MismatchedMetadataChildWsResult();
	result.setChildName(mismatchedChild.get("col1"));
	result.setParentName(mismatchedChild.get("col2"));
	result.setParentType(mismatchedChild.get("col3"));

	String auId = PluginManager.generateAuId(mismatchedChild.get("col5"),
	    mismatchedChild.get("col4"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = getPluginManager().getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  result.setAuName(au.getName());
	} else {
	  result.setAuName(auId);
	}

	mismatchedChildren.add(result);
      }

      return mismatchedChildren;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the book chapters in the database whose parent is not a book or a
   * book series.
   * 
   * @return a List<MismatchedChildWsResult> with the mismatched book chapters
   *         sorted by Archival Unit, parent name and child name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<MismatchedMetadataChildWsResult> getMismatchedParentBookChapters()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getMismatchedParentBookChapters(): ";
    List<MismatchedMetadataChildWsResult> mismatchedChildren =
	new ArrayList<MismatchedMetadataChildWsResult>();

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      for (Map<String, String> mismatchedChild :
	getMetadataManager().getMismatchedParentBookChapters()) {
	MismatchedMetadataChildWsResult result =
	    new MismatchedMetadataChildWsResult();
	result.setChildName(mismatchedChild.get("col1"));
	result.setParentName(mismatchedChild.get("col2"));
	result.setParentType(mismatchedChild.get("col3"));

	String auId = PluginManager.generateAuId(mismatchedChild.get("col5"),
	    mismatchedChild.get("col4"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = getPluginManager().getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  result.setAuName(au.getName());
	} else {
	  result.setAuName(auId);
	}

	mismatchedChildren.add(result);
      }

      return mismatchedChildren;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the book volumes in the database whose parent is not a book or a
   * book series.
   * 
   * @return a List<MismatchedChildWsResult> with the mismatched book volumes
   *         sorted by Archival Unit, parent name and child name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<MismatchedMetadataChildWsResult> getMismatchedParentBookVolumes()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getMismatchedParentBookVolumes(): ";
    List<MismatchedMetadataChildWsResult> mismatchedChildren =
	new ArrayList<MismatchedMetadataChildWsResult>();

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      for (Map<String, String> mismatchedChild :
	getMetadataManager().getMismatchedParentBookChapters()) {
	MismatchedMetadataChildWsResult result =
	    new MismatchedMetadataChildWsResult();
	result.setChildName(mismatchedChild.get("col1"));
	result.setParentName(mismatchedChild.get("col2"));
	result.setParentType(mismatchedChild.get("col3"));

	String auId = PluginManager.generateAuId(mismatchedChild.get("col5"),
	    mismatchedChild.get("col4"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = getPluginManager().getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  result.setAuName(au.getName());
	} else {
	  result.setAuName(auId);
	}

	mismatchedChildren.add(result);
      }

      return mismatchedChildren;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publishers for the Archival Units in the database with
   * multiple publishers.
   * 
   * @return a List<KeyValueListPair> with the publishers keyed by the Archival
   *         Unit identifier.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getAuIdsWithMultiplePublishers()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuIdsWithMultiplePublishers(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publishers linked to the Archival Units.
      Map<String, Collection<String>> ausPublishers =
	  getMetadataManager().getAuIdsWithMultiplePublishers();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "ausPublishers.size() = "
	  + ausPublishers.size());

      // Check whether there are results to display.
      if (ausPublishers.size() > 0) {
        // Yes: Loop through the Archival Unit identifiers.
        for (String auId : ausPublishers.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

          ArrayList<String> publishers = new ArrayList<String>();

          for (String publisher : ausPublishers.get(auId)) {
            if (log.isDebug3())
              log.debug3(DEBUG_HEADER + "publisher = " + publisher);
            publishers.add(publisher);
          }

          results.add(new KeyValueListPair(auId, publishers));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publishers for the Archival Units in the database with
   * multiple publishers.
   * 
   * @return a List<KeyValueListPair> with the publishers keyed by the Archival
   *         Unit name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getAuNamesWithMultiplePublishers()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuNamesWithMultiplePublishers(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publishers linked to the Archival Units.
      Map<String, Collection<String>> ausPublishers =
	  getMetadataManager().getAuNamesWithMultiplePublishers();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "ausPublishers.size() = "
	  + ausPublishers.size());

      // Check whether there are results to display.
      if (ausPublishers.size() > 0) {
        // Yes: Loop through the Archival Unit names.
        for (String auName : ausPublishers.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auName = " + auName);

          ArrayList<String> publishers = new ArrayList<String>();

          for (String publisher : ausPublishers.get(auName)) {
            if (log.isDebug3())
              log.debug3(DEBUG_HEADER + "publisher = " + publisher);
            publishers.add(publisher);
          }

          results.add(new KeyValueListPair(auName, publishers));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the metadata items in the database that do not have a name.
   * 
   * @return a List<UnnamedItemWsResult> with the unnamed metadata items sorted
   *         sorted by publisher, Archival Unit, parent type, parent name and
   *         item type.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<UnnamedItemWsResult> getUnnamedItems()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getUnnamedItems(): ";
    List<UnnamedItemWsResult> unnamedItems =
	new ArrayList<UnnamedItemWsResult>();

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      for (Map<String, String> unnamedItem :
	getMetadataManager().getUnnamedItems()) {
	UnnamedItemWsResult result = new UnnamedItemWsResult();
	result.setItemCount(Integer.valueOf(unnamedItem.get("col1")));
	result.setItemType(unnamedItem.get("col2"));
	result.setParentName(unnamedItem.get("col3"));
	result.setParentType(unnamedItem.get("col4"));

	String auId = PluginManager.generateAuId(unnamedItem.get("col6"),
	    unnamedItem.get("col5"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = getPluginManager().getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  result.setAuName(au.getName());
	} else {
	  result.setAuName(auId);
	}

	result.setPublisherName(unnamedItem.get("col7"));

	unnamedItems.add(result);
      }

      return unnamedItems;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the proprietary identifiers for the publications in the database
   * with multiple proprietary identifiers.
   * 
   * @return a List<KeyValueListPair> with the proprietary identifiers keyed by
   *         the publication name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getPublicationsWithMultiplePids()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublicationsWithMultiplePids(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the proprietary identifiers linked to the publications.
      Map<String, Collection<String>> publicationsPids =
	  getMetadataManager().getPublicationsWithMultiplePids();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "publicationsPids.size() = " + publicationsPids.size());

      // Check whether there are results to display.
      if (publicationsPids.size() > 0) {
        // Yes: Loop through the publications.
        for (String publicationName : publicationsPids.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

          ArrayList<String> pids = new ArrayList<String>();

          for (String pid : publicationsPids.get(publicationName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pid = " + pid);
            pids.add(pid);
          }

          results.add(new KeyValueListPair(publicationName, pids));
        }
      }

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "results.size() = " + results.size());
      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the non-parent metadata items in the database that have no DOI.
   *
   * @return a List<MetadataItemWsResult> with the non-parent metadata items
   *         that have no DOI sorted sorted by publisher, Archival Unit, parent
   *         type, parent name, item type and item name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<MetadataItemWsResult> getNoDoiItems()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getNoDoiItems(): ";
    List<MetadataItemWsResult> noDoiItems =
	new ArrayList<MetadataItemWsResult>();

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      for (Map<String, String> noDoiItem :
	getMetadataManager().getNoDoiItems()) {
	MetadataItemWsResult result = new MetadataItemWsResult();
	result.setItemName(noDoiItem.get("col1"));
	result.setItemType(noDoiItem.get("col2"));
	result.setParentName(noDoiItem.get("col3"));
	result.setParentType(noDoiItem.get("col4"));

	String auId = PluginManager.generateAuId(noDoiItem.get("col6"),
	    noDoiItem.get("col5"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = getPluginManager().getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  result.setAuName(au.getName());
	} else {
	  result.setAuName(auId);
	}

	result.setPublisherName(noDoiItem.get("col7"));

	noDoiItems.add(result);
      }

      return noDoiItems;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the non-parent metadata items in the database that have no Access
   * URL.
   *
   * @return a List<MetadataItemWsResult> with the non-parent metadata items
   *         that have no Access URL sorted sorted by publisher, Archival Unit,
   *         parent type, parent name, item type and item name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<MetadataItemWsResult> getNoAccessUrlItems()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getNoAccessUrlItems(): ";
    List<MetadataItemWsResult> noAccessUrlItems =
	new ArrayList<MetadataItemWsResult>();

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      for (Map<String, String> noAccessUrlItem :
	getMetadataManager().getNoAccessUrlItems()) {
	MetadataItemWsResult result = new MetadataItemWsResult();
	result.setItemName(noAccessUrlItem.get("col1"));
	result.setItemType(noAccessUrlItem.get("col2"));
	result.setParentName(noAccessUrlItem.get("col3"));
	result.setParentType(noAccessUrlItem.get("col4"));

	String auId = PluginManager.generateAuId(noAccessUrlItem.get("col6"),
	    noAccessUrlItem.get("col5"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = getPluginManager().getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  result.setAuName(au.getName());
	} else {
	  result.setAuName(auId);
	}

	result.setPublisherName(noAccessUrlItem.get("col7"));

	noAccessUrlItems.add(result);
      }

      return noAccessUrlItems;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the Archival Units in the database with no metadata items.
   * 
   * @return a List<String> with the sorted Archival Unit names.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<String> getNoItemsAuIds() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getNoItemsAuIds(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      return ((List<String>)(getMetadataManager().getNoItemsAuIds()));
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the metadata information of an archival unit in the system.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @return an AuMetadataWsResult with the metadata information of the archival
   *         unit.
   * @throws LockssWebServicesFault
   */
  @Override
  public AuMetadataWsResult getAuMetadata(String auId)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuMetadata(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    // Input validation.
    if (StringUtil.isNullString(auId)) {
      throw new LockssWebServicesFault(
	  new IllegalArgumentException("Invalid Archival Unit identifier"),
	  new LockssWebServicesFaultInfo("Archival Unit identifier = " + auId));
    }

    try {
      AuMetadataWsResult result = new AuMetadataWsResult();
      result.setAuId(auId);

      // Get the metadata.
      Map<String, Object> sqlResult = getMetadataManager().getAuMetadata(auId);

      if (sqlResult != null) {
	result.setAuMdSeq((Long)sqlResult.get(AU_MD_SEQ_COLUMN));
	result.setAuSeq((Long)sqlResult.get(AU_SEQ_COLUMN));
	result.setMdVersion((Integer)sqlResult.get(MD_VERSION_COLUMN));
	result.setExtractTime((Long)sqlResult.get(EXTRACT_TIME_COLUMN));
	result.setCreationTime((Long)sqlResult.get(CREATION_TIME_COLUMN));
	result.setProviderSeq((Long)sqlResult.get(PROVIDER_SEQ_COLUMN));
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);

      return result;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the Archival Units that exist in the database but that have been
   * deleted from the daemon.
   * 
   * @return a List<AuMetadataWsResult> with the Archival unit data.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<AuMetadataWsResult> getDbArchivalUnitsDeletedFromDaemon()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getDbArchivalUnitsDeletedFromDaemon(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      Collection<Map<String, Object>> dbResults =
	  getMetadataManager().getDbArchivalUnitsDeletedFromDaemon();

      if (dbResults == null) {
	return null;
      }

      List<AuMetadataWsResult> results = new ArrayList<AuMetadataWsResult>();

      for (Map<String, Object> auProperties : dbResults) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "auProperties = " + auProperties);

	AuMetadataWsResult result = new AuMetadataWsResult();

	result.setAuId(PluginManager.generateAuId(
	    (String)auProperties.get(PLUGIN_ID_COLUMN),
	    (String)auProperties.get(AU_KEY_COLUMN)));

	result.setAuSeq((Long)auProperties.get(AU_SEQ_COLUMN));
	result.setMdVersion((Integer)auProperties.get(MD_VERSION_COLUMN));
	result.setExtractTime((Long)auProperties.get(EXTRACT_TIME_COLUMN));
	result.setCreationTime((Long)auProperties.get(CREATION_TIME_COLUMN));
	result.setProviderName((String)auProperties.get(PROVIDER_NAME_COLUMN));
	result.setItemCount((Integer)auProperties.get("item_count"));

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);

	results.add(result);
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = " + results);
      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the metadata manager.
   * 
   * @return a MetadataManager with the metadata manager.
   */
  private MetadataManager getMetadataManager() {
    return (MetadataManager)(LockssApp.getManager(
	LockssDaemon.METADATA_MANAGER));
  }

  /**
   * Provides the plugin manager.
   * 
   * @return a PluginManager with the plugin manager.
   */
  private PluginManager getPluginManager() {
    return (PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
  }
}
