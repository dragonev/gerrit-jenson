package com.dragon.gerrit.jenson.data.converter;

public interface TagFormatter<E extends Enum> {
    String getValueToReplace(E tag);
    String getMessage();
}
