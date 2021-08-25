package org.omegat.plugins.vimish;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// This annotation will prevent the Jackson unmarshaller
// from throwing an exception if we add fields in the future
// that are not contained in the user's configuration file
@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigurationData {
  public boolean moveCursorBack;
  public Map<String, String> keyMappings;
  public Map<String, String> keyChords;
  public Map<String, String> abbreviations;
}
