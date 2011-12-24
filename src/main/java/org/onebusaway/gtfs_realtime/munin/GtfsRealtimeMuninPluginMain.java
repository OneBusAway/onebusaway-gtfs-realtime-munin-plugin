/**
 * Copyright (C) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs_realtime.munin;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtimeOneBusAway;

public class GtfsRealtimeMuninPluginMain {

  private static final String KEY_TRIP_UPDATES_URL = "tripUpdatesUrl";

  private static final String KEY_VEHICLE_POSITIONS_URL = "vehiclePositionsUrl";

  private static final String KEY_SOURCE = "source";

  private static final String KEY_CATEGORY = "category";

  private static final ExtensionRegistry _registry = ExtensionRegistry.newInstance();

  static {
    _registry.add(GtfsRealtimeOneBusAway.delay);
    _registry.add(GtfsRealtimeOneBusAway.source);
  }

  public static void main(String[] args) throws IOException {
    GtfsRealtimeMuninPluginMain m = new GtfsRealtimeMuninPluginMain();
    m.parseEnvironment();

    if (args.length == 1 && args[0].equals("config")) {
      m.config();
    } else if (args.length == 0) {
      m.fetch();
    }
  }

  private URL _tripUpdatesUrl;

  private URL _vehiclePositionsUrl;

  private String _source;

  private String _category = "gtfs_realtime";

  public void parseEnvironment() throws MalformedURLException {
    Map<String, String> env = System.getenv();
    if (env.containsKey(KEY_TRIP_UPDATES_URL)) {
      _tripUpdatesUrl = new URL(env.get(KEY_TRIP_UPDATES_URL));
    }
    if (env.containsKey(KEY_VEHICLE_POSITIONS_URL)) {
      _vehiclePositionsUrl = new URL(env.get(KEY_VEHICLE_POSITIONS_URL));
    }
    if (env.containsKey(KEY_SOURCE)) {
      _source = env.get(KEY_SOURCE);
    }
    if (env.containsKey(KEY_CATEGORY)) {
      _category = env.get(KEY_CATEGORY);
    }
  }

  public void config() {
    String title = "GTFS-realtime Entity Counts";
    if (_source != null) {
      title += " - " + _source;
    }
    System.out.println("graph_title " + title);
    System.out.println("graph_info GTFS-realtime feed statistics");
    if (_category != null) {
      System.out.println("graph_category " + _category);
    }
    if (_tripUpdatesUrl != null) {
      System.out.println("trips.label Trips");
      System.out.println("trips.info Count of TripUpdate entries");
      System.out.println("trips.min 0");
    }
    if (_vehiclePositionsUrl != null) {
      System.out.println("vehicles.label Vehicles");
      System.out.println("vehicles.info Count of VehiclePosition entries");
      System.out.println("vehicles.min 0");
    }
  }

  public void fetch() throws IOException {
    if (_tripUpdatesUrl != null) {
      InputStream in = _tripUpdatesUrl.openStream();
      FeedMessage message = FeedMessage.parseFrom(in, _registry);
      FieldDescriptor fieldDesc = FeedEntity.getDescriptor().findFieldByName(
          "trip_update");
      int count = countEntities(message, fieldDesc);
      System.out.println("trips.value " + count);
    }
    if (_vehiclePositionsUrl != null) {
      InputStream in = _vehiclePositionsUrl.openStream();
      FeedMessage message = FeedMessage.parseFrom(in, _registry);
      FieldDescriptor fieldDesc = FeedEntity.getDescriptor().findFieldByName(
          "vehicle");
      int count = countEntities(message, fieldDesc);
      System.out.println("vehicles.value " + count);
    }
  }

  private int countEntities(FeedMessage message, FieldDescriptor desc) {
    int count = 0;
    for (int i = 0; i < message.getEntityCount(); ++i) {
      FeedEntity entity = message.getEntity(i);
      if (!entity.hasField(desc)) {
        continue;
      }
      if (!isSourceMatched(entity)) {
        continue;
      }
      count++;
    }
    return count;
  }

  private boolean isSourceMatched(FeedEntity entity) {
    if (_source == null)
      return true;

    if (!entity.hasExtension(GtfsRealtimeOneBusAway.source)) {
      return false;
    }
    String source = entity.getExtension(GtfsRealtimeOneBusAway.source);
    return _source.equals(source);
  }
}
