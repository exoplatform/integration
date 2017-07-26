package org.exoplatform.social.plugin.doc;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

public class ComposerFileItem implements Serializable, Comparable<ComposerFileItem> {
  private static final long serialVersionUID = -290642886983269011L;

  private static long       sharedIndice;

  private String            name;

  private String            title;

  private String            id;

  private String            mimeType;

  private String            nodeIcon;

  private String            link;

  private String            size;

  private String            path;

  private double            sizeInBytes;

  private String            resolverType;
  
  private long              indice;

  public ComposerFileItem() {
    setIndice(sharedIndice++);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public double getSizeInBytes() {
    return sizeInBytes;
  }

  public void setSizeInBytes(double sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
  }

  public String getResolverType() {
    return resolverType;
  }

  public void setResolverType(String resolverType) {
    this.resolverType = resolverType;
  }

  public String getNodeIcon() {
    return nodeIcon;
  }

  public void setNodeIcon(String nodeIcon) {
    this.nodeIcon = nodeIcon;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  public long getIndice() {
    return indice;
  }

  public void setIndice(long indice) {
    this.indice = indice;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ComposerFileItem)) {
      return false;
    }
    ComposerFileItem fileItem = (ComposerFileItem) obj;
    return StringUtils.equals(fileItem.getTitle(), getTitle());
  }

  @Override
  public int hashCode() {
    if (StringUtils.isBlank(title)) {
      return super.hashCode();
    }
    return title.hashCode();
  }

  @Override
  public int compareTo(ComposerFileItem o) {
    return (int) (getIndice() - o.getIndice());
  }
}