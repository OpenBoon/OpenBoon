# Default Plugin Search Path

The './plugins' directory is the default plugins search path.  Archivist will look for zip files
in this directory, unpack them, and install them into shared/plugins, which is the place where
the analysts can find them.

The install-plugins.sh script will copy anything from your java-plugin-sdk checkout's dist folder into
the plugin folder.


