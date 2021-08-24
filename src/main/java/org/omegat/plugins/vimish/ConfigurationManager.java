package org.omegat.plugins.vimish;

import org.omegat.util.Log;
import org.omegat.util.StaticUtils;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.JsonParseException;

class ConfigurationManager {
  private final String CONFIGURATION_FILENAME = "VimishConfig.json";
  public File configurationFile = new File(StaticUtils.getConfigDir(), CONFIGURATION_FILENAME);
  private ObjectMapper objectMapper = new ObjectMapper();
  private static ConfigurationManager instance;
  Configuration configuration;

  String DESERIALIZATION_ERROR_MESSAGE =
    "Your configuration file (" + configurationFile.getPath() + ") " + 
    "appears to be corrupted. Default configuration will be loaded. " +
    "To stop seeing this message, try to fix the file, or delete it so " +
    "that a new one may be automatically created.";

  String LOAD_ERROR_MESSAGE =
    "Your configuration file (" + configurationFile.getPath() + ") " + 
    "could not be loaded. Default configuration will be used.";

  boolean DEFAULT_MOVE_CURSOR_BACK = true;
                                  
  private ConfigurationManager() {
    configuration = getConfiguration();
  }

  static ConfigurationManager getConfigurationManager() {
    if (instance == null) {
      instance = new ConfigurationManager();
    }
    return instance;
  } 

  Configuration getConfiguration() {
    Configuration configuration = null;
    if (configurationFile.exists()) {
      try {
        configuration = objectMapper.readValue(configurationFile, Configuration.class); 
      } catch(JsonMappingException | JsonParseException exception) {
        Log.log("Unable to deserialize Json configuration file: " + exception);
        JOptionPane.showMessageDialog(null, DESERIALIZATION_ERROR_MESSAGE);
      } catch(IOException exception) {
        Log.log("Unable to load Json config file: " + exception);
        JOptionPane.showMessageDialog(null, LOAD_ERROR_MESSAGE);
      }
    }

    // Load defaults if no valid configuration found
    if (configuration == null) {
      configuration = getDefaultConfiguration();
    }

    return configuration;
  }

  private Configuration getDefaultConfiguration() {
    Configuration configuration = new Configuration();
    configuration.moveCursorBack = DEFAULT_MOVE_CURSOR_BACK;
    return configuration;
  }

}

