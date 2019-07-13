package me.w1992wishes.hbase.inaction.coprocessors;

import com.google.protobuf.Service;

import java.io.IOException;

public interface RelationCountProtocol extends Service {
  public long followedByCount(String userId) throws IOException;
}