
import { ExpoConfig } from "expo/config";


module.exports = ({ config }) => {
  plugins: [
    ["./plugins/withPlugin.ts"],
  ],
};
