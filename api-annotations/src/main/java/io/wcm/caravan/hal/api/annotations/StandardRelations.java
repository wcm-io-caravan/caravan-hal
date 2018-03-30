/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.annotations;

/**
 * Constants for the standard link relations used in the SDL REST APIs
 * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA Link Relation Types</a>
 */
public final class StandardRelations {

  private StandardRelations() {
    // constants only
  }

  /**
   * Refers to a substitute for this context
   * @see <a href="http://www.w3.org/TR/html5/links.html#link-type-alternate">HTML5 link-type-alternate</a>
   */
  public static final String ALTERNATE = "alternate";

  /**
   * Designates the preferred version of a resource (the IRI and its contents).
   * @see <a href="http://www.iana.org/go/rfc6596">RFC6596</a>
   */
  public static final String CANONICAL = "canonical";

  /**
   * The target IRI points to a resource which represents the collection resource for the context IRI.
   * @see <a href="http://www.iana.org/go/rfc6573">RFC6573</a>
   */
  public static final String COLLECTION = "collection";

  /**
   * An IRI that refers to the furthest preceding resource in a series of resources.
   * @see <a href="http://www.iana.org/go/rfc5988">RFC5988</a>
   */
  public static final String FIRST = "first";

  /**
   * Refers to an index.
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224">HTML 4.01 Specification</a>
   */
  public static final String INDEX = "index";

  /**
   * The target IRI points to a resource that is a member of the collection represented by the context IRI.
   * @see <a href="http://www.iana.org/go/rfc6573">RFC6573</a>
   */
  public static final String ITEM = "item";

  /**
   * An IRI that refers to the furthest following resource in a series of resources.
   * @see <a href="http://www.iana.org/go/rfc5988">RFC5988</a>
   */
  public static final String LAST = "last";

  /**
   * Indicates that the link's context is a part of a series, and that the next in the series is the link target.
   * @see <a href="http://www.w3.org/TR/html5/links.html#link-type-next">HTML5 Recommendation</a>
   */
  public static final String NEXT = "next";

  /**
   * Indicates that the link's context is a part of a series, and that the previous in the series is the link target.
   * @see <a href="http://www.w3.org/TR/html5/links.html#link-type-next">HTML5 Recommendation</a>
   */
  public static final String PREV = "prev";

  /**
   * Identifies a related resource.
   * @see <a href="http://www.iana.org/go/rfc4287">RFC4287</a>
   */
  public static final String RELATED = "related";

  /**
   * section Refers to a section in a collection of resources.
   */
  public static final String SECTION = "section";

  /**
   * Conveys an identifier for the link's context.
   * @see <a href="http://www.iana.org/go/rfc4287">RFC4287</a>
   */
  public static final String SELF = "self";

  /**
   * Refers to a parent document in a hierarchy of documents.
   * @see <a href="http://www.iana.org/go/rfc5988">RFC5988</a>
   */
  public static final String UP = "up";

  /**
   * Identifies a resource that is the source of the information in the link's context.
   * @see <a href="http://www.iana.org/go/rfc4287">RFC4287</a>
   */
  public static final String VIA = "via";

}
