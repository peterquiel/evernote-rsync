package com.stm.evenote.rsync.model;

import java.util.Objects;

public class SyncPath {

  public static final SyncPath EMPTY = new SyncPath(null, null);
  private static final String pathSeparator = System.getProperty("file.separator");
  private final String segmentName;
  private final SyncPath parentPath;

  private static String removeEndingSlash(String segmentName) {
    if (segmentName != null && segmentName.endsWith("/") || segmentName.endsWith(pathSeparator)) {
      return segmentName.substring(0, segmentName.length()-1);
    }
    return segmentName;
  }

  private static String removeStartingSlash(String segmentName) {
    if (segmentName != null && segmentName.startsWith("/") || segmentName.startsWith(pathSeparator)) {
      return segmentName.substring(1);
    }
    return segmentName;
  }

  SyncPath(SyncPath parentPath, String segmentName) {

    this.segmentName = segmentName;
    this.parentPath = parentPath;

  }

  public SyncPath prepend(String parentPath) {
    if (parentPath == null || parentPath.trim().length() < 1) {
      return this;
    }
    return new SyncPath(null, removeEndingSlash(parentPath)).append(this.toString());
  }

  public SyncPath append(String childPath) {
    if (childPath == null || childPath.trim().length() < 1) {
      return this;
    }
    return new SyncPath(this, removeStartingSlash(removeEndingSlash(childPath)));
  }

  @Override
  public int hashCode() {
    int result = segmentName != null ? segmentName.hashCode() : 0;
    result = 31 * result + (parentPath != null ? parentPath.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return Objects.equals(this.toString(), o.toString());
  }

  public String toString() {
    if (parentPath == null) {
      return segmentName == null ? "" : segmentName;
    }

    String path = parentPath.toString();
    if (segmentName != null && segmentName.trim().length() > 1) {
      if (path != null && path.trim().length() > 0) {
        path = path + pathSeparator;
      }
      path = path  + segmentName;
    }
    return path;
  }
}
