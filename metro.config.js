const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');
const path = require('path');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const config = {
  resolver: {
    extraNodeModules: {
      'make-plural': path.resolve(__dirname, 'node_modules/make-plural'),
    },
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
