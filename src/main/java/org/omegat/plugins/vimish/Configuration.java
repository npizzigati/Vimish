package org.omegat.plugins.vimish;

import org.omegat.util.Log;
import org.omegat.util.StaticUtils;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;

class Configuration {
  private final String CONFIGURATION_FILENAME = "VimishConfig.json";
  public File configurationFile = new File(StaticUtils.getConfigDir(), CONFIGURATION_FILENAME);
  // INDENT_OUTPUT enables json pretty printing
  private ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  private static Configuration instance;
  private ConfigurationData configurationData;
  private boolean keyTablesNeedRefreshing = false;

  String DESERIALIZATION_ERROR_MESSAGE =
    "Your configuration file (" + configurationFile.getPath() + ") " +
    "appears to be corrupted. Default configuration will be loaded.";

  String LOAD_ERROR_MESSAGE =
    "Your configuration file (" + configurationFile.getPath() + ") " +
    "could not be loaded. Default configuration will be used.";

  boolean DEFAULT_MOVE_CURSOR_BACK = true;
  boolean DEFAULT_USE_SYSTEM_CLIPBOARD = true;
  KeyMappings DEFAULT_KEY_MAPPINGS = new KeyMappings();
  KeyChords DEFAULT_KEY_CHORDS = new KeyChords();
  Map<String, String> DEFAULT_ABBREVIATIONS = new HashMap<String, String>();

  private Configuration() {
    readFromFile();
  }

  static Configuration getConfiguration() {
    if (instance == null) {
      instance = new Configuration();
    }
    return instance;
  }

  void refresh() {
    readFromFile();
  }

  public void flagKeyTablesRefreshNeeded() {
    keyTablesNeedRefreshing = true;
  }

  public void flagKeyTablesRefreshNotified() {
    keyTablesNeedRefreshing = false;
  }

  public boolean keyTablesNeedRefreshing() {
    return keyTablesNeedRefreshing;
  }

  boolean getConfigMoveCursorBack() {
    return configurationData.moveCursorBack;
  }

  boolean getConfigUseSystemClipboard() {
    return configurationData.useSystemClipboard;
  }

  KeyMappings getKeyMappings() {
    KeyMappings keyMappings = configurationData.keyMappings;
    if (keyMappings == null) {
      keyMappings = DEFAULT_KEY_MAPPINGS;
    }

    return keyMappings;
  }

  KeyChords getKeyChords() {
    KeyChords keyChords = configurationData.keyChords;
    if (keyChords == null) {
      keyChords = DEFAULT_KEY_CHORDS;
    }

    return keyChords;
  }

  void readFromFile() {
    if (configurationFile.exists()) {
      try {
        configurationData = objectMapper.readValue(configurationFile, ConfigurationData.class);
      } catch(JsonMappingException | JsonParseException jpe) {
        Log.log("Unable to deserialize Json configuration file: " + jpe);
        JOptionPane.showMessageDialog(null, DESERIALIZATION_ERROR_MESSAGE);
        configurationData = getDefaultConfigurationData();
      } catch(IOException ioe) {
        Log.log("Unable to load Json config file: " + ioe);
        JOptionPane.showMessageDialog(null, LOAD_ERROR_MESSAGE);
        configurationData = getDefaultConfigurationData();
      }
    } else {
      Log.log("Setting configuration data to default since no configuration file " +
              "was found.");
      configurationData = getDefaultConfigurationData();
    }
  }

  void writeToFile(ConfigurationData newData) {
    try {
      objectMapper.writeValue(configurationFile, newData);
    } catch(JsonProcessingException jpe) {
      Log.log("Unable to create JSON: " + jpe);
    } catch(IOException ioe) {
      Log.log("Unable to write Vimish preferences file: " + ioe);
    }

    refresh();
  }

  private ConfigurationData getDefaultConfigurationData() {
    ConfigurationData configurationData = new ConfigurationData();
    configurationData.moveCursorBack = DEFAULT_MOVE_CURSOR_BACK;
    configurationData.useSystemClipboard = DEFAULT_USE_SYSTEM_CLIPBOARD;
    configurationData.keyMappings = DEFAULT_KEY_MAPPINGS;
    configurationData.keyChords = DEFAULT_KEY_CHORDS;
    configurationData.abbreviations = DEFAULT_ABBREVIATIONS;
    return configurationData;
  }

}
