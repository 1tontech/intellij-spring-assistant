package in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isEmpty;


// Code borrowed from https://github.com/spring-io/initializr/blob/master/initializr-generator


/**
 * Define the version number of a module. A typical version is represented
 * as {@code MAJOR.MINOR.PATCH.QUALIFIER} where the qualifier can have an
 * extra version.
 * <p>
 * For example: {@code 1.2.0.RC1} is the first release candidate of 1.2.0
 * and {@code 1.5.0.M4} is the fourth milestone of 1.5.0. The special
 * {@code RELEASE} qualifier indicates a final release (a.k.a. GA)
 * <p>
 * The main purpose of parsing a version is to compare it with another
 * version, see {@link Comparable}.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public final class Version implements Serializable, Comparable<Version> {

  private static final VersionQualifierComparator qualifierComparator =
      new VersionQualifierComparator();

  private static final VersionParser parser = new VersionParser(Collections.emptyList());

  private final Integer major;
  private final Integer minor;
  private final Integer patch;
  private final Qualifier qualifier;

  // For Jackson
  @SuppressWarnings("unused")
  private Version() {
    this(null, null, null, null);
  }

  public Version(Integer major, Integer minor, Integer patch, Qualifier qualifier) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.qualifier = qualifier;
  }

  /**
   * Parse the string representation of a {@link Version}. Throws an
   * {@link InvalidVersionException} if the version could not be parsed.
   *
   * @param text the version text
   * @return a Version instance for the specified version text
   * @throws InvalidVersionException if the version text could not be parsed
   * @see VersionParser
   */
  public static Version parse(String text) {
    return parser.parse(text);
  }

  /**
   * Parse safely the specified string representation of a {@link Version}.
   * <p>
   * Return {@code null} if the text represents an invalid version.
   *
   * @param text the version text
   * @return a Version instance for the specified version text
   * @see VersionParser
   */
  public static Version safeParse(String text) {
    try {
      return parse(text);
    } catch (InvalidVersionException e) {
      return null;
    }
  }

  private static int safeCompare(Integer first, Integer second) {
    Integer firstIndex = first != null ? first : 0;
    Integer secondIndex = second != null ? second : 0;
    return firstIndex.compareTo(secondIndex);
  }

  public Integer getMajor() {
    return major;
  }

  public Integer getMinor() {
    return minor;
  }

  public Integer getPatch() {
    return patch;
  }

  public Qualifier getQualifier() {
    return qualifier;
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + patch + (qualifier != null ?
        "." + qualifier.qualifier + (qualifier.version != null ? qualifier.version : "") :
        "");
  }

  @Override
  public int compareTo(Version other) {
    if (other == null) {
      return 1;
    }
    int majorDiff = safeCompare(this.major, other.major);
    if (majorDiff != 0) {
      return majorDiff;
    }
    int minorDiff = safeCompare(this.minor, other.minor);
    if (minorDiff != 0) {
      return minorDiff;
    }
    int patch = safeCompare(this.patch, other.patch);
    if (patch != 0) {
      return patch;
    }
    return qualifierComparator.compare(this.qualifier, other.qualifier);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((major == null) ? 0 : major.hashCode());
    result = prime * result + ((minor == null) ? 0 : minor.hashCode());
    result = prime * result + ((patch == null) ? 0 : patch.hashCode());
    result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Version other = (Version) obj;
    if (major == null) {
      if (other.major != null)
        return false;
    } else if (!major.equals(other.major))
      return false;
    if (minor == null) {
      if (other.minor != null)
        return false;
    } else if (!minor.equals(other.minor))
      return false;
    if (patch == null) {
      if (other.patch != null)
        return false;
    } else if (!patch.equals(other.patch))
      return false;
    if (qualifier == null) {
      return other.qualifier == null;
    } else
      return qualifier.equals(other.qualifier);
  }


  public static class Qualifier implements Serializable {

    private String qualifier;
    private Integer version;

    public Qualifier(String qualifier) {
      this.qualifier = qualifier;
    }

    public String getQualifier() {
      return qualifier;
    }

    public void setQualifier(String qualifier) {
      this.qualifier = qualifier;
    }

    public Integer getVersion() {
      return version;
    }

    public void setVersion(Integer version) {
      this.version = version;
    }

    @Override
    public String toString() {
      return "Qualifier [" + (qualifier != null ? "qualifier=" + qualifier + ", " : "") + (
          version != null ? "version=" + version : "") + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Qualifier other = (Qualifier) obj;
      if (qualifier == null) {
        if (other.qualifier != null)
          return false;
      } else if (!qualifier.equals(other.qualifier))
        return false;
      if (version == null) {
        return other.version == null;
      } else
        return version.equals(other.version);
    }
  }


  private static class VersionQualifierComparator implements Comparator<Qualifier> {

    static final String RELEASE = "RELEASE";
    static final String SNAPSHOT = "BUILD-SNAPSHOT";
    static final String MILESTONE = "M";
    static final String RC = "RC";

    static final List<String> KNOWN_QUALIFIERS = Arrays.asList(MILESTONE, RC, SNAPSHOT, RELEASE);

    private static int compareQualifierVersion(Qualifier first, Qualifier second) {
      Integer firstVersion = first.getVersion() != null ? first.getVersion() : 0;
      Integer secondVersion = second.getVersion() != null ? second.getVersion() : 0;
      return firstVersion.compareTo(secondVersion);
    }

    private static int compareQualifier(Qualifier first, Qualifier second) {
      Integer firstIndex = getQualifierIndex(first.qualifier);
      Integer secondIndex = getQualifierIndex(second.qualifier);

      // Unknown qualifier, alphabetic ordering
      if (firstIndex == -1 && secondIndex == -1) {
        return first.qualifier.compareTo(second.qualifier);
      } else {
        return firstIndex.compareTo(secondIndex);
      }
    }

    private static int getQualifierIndex(String qualifier) {
      return !isEmpty(qualifier) ? KNOWN_QUALIFIERS.indexOf(qualifier) : 0;
    }

    @Override
    public int compare(Qualifier o1, Qualifier o2) {
      Qualifier first = o1 != null ? o1 : new Qualifier(RELEASE);
      Qualifier second = o2 != null ? o2 : new Qualifier(RELEASE);

      int qualifier = compareQualifier(first, second);
      return qualifier != 0 ? qualifier : compareQualifierVersion(first, second);
    }
  }

}
