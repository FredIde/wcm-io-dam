/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.dam.assetservice.impl;

import java.util.Calendar;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO8601;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.day.cq.dam.api.DamEvent;

/**
 * Handles list of configured DAM paths and listens to DAM events on this paths to generate
 * a new data version on each DAM content change relevant for the DAM asset services consumers.
 */
@Component(immediate = true)
@Service({
  DamPathHandler.class, EventHandler.class
})
public class DamPathHandler implements EventHandler {

  private volatile Pattern damPathsPattern;
  private volatile String dataVersion;

  @Activate
  protected void activate() {
    generateNewDataVersion();
  }

  /**
   * Set DAM paths that should be handled. Only called once by {@link AssetServiceServlet}.
   * @param damPaths DAM folder paths or empty/null if all should be handled.
   */
  public void setDamPaths(String[] damPaths) {
    if (damPaths == null || damPaths.length == 0) {
      damPathsPattern = null;
    }
    else {
      StringBuilder pattern = new StringBuilder();
      pattern.append("^(");
      for (int i = 0; i < damPaths.length; i++) {
        if (i > 0) {
          pattern.append("|");
        }
        pattern.append(Pattern.quote(damPaths[i]));
        pattern.append("/.*");
      }
      pattern.append(")$");
      damPathsPattern = Pattern.compile(pattern.toString());
    }
  }

  /**
   * Checks if the given DAM asset is allowed to process.
   * @param assetPath Asset path
   * @return true if processing is allowed.
   */
  public boolean isAllowedAsset(String assetPath) {
    if (damPathsPattern == null) {
      return true;
    }
    else {
      return damPathsPattern.matcher(assetPath).matches();
    }
  }

  /**
   * Get current data version for all allowed assets.
   * @return Data version
   */
  public String getDataVersion() {
    return dataVersion;
  }

  @Override
  public void handleEvent(Event event) {
    if (!StringUtils.equals(event.getTopic(), DamEvent.EVENT_TOPIC)) {
      return;
    }
    DamEvent damEvent = DamEvent.fromEvent(event);
    if (isAllowedAsset(damEvent.getAssetPath())) {
      // generate a new data version on any DAM event affecting any of the configured paths
      generateNewDataVersion();
    }
  }

  /**
   * Generates a new data version based on current timestamp.
   */
  private void generateNewDataVersion() {
    dataVersion = ISO8601.format(Calendar.getInstance());
  }

}
