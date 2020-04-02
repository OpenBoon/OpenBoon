#Instructions
## In docker-compose
* Volumes must be mounted in ${VOLUME_ABSOLUTE_PATH}:/mnt
* Exchange ${VOLUME_ABSOLUTE_PATH} to your path
* Mount as many volumes as you want

## In pylib/properties.yml
* [mounts] : List of folders that crawler will run over
* [supported_types] : Extensions that will be considered to import
* [batch_size] : Minimum batch size to be uploaded
* [api_key] : API KEY
* [zmlp_server] : ZMLP Server URL
* [sqlite_db] : SQLite3 database location path

## Sqlite.db
##### SQLite3 database that stores reference to already uploaded files
##### Tables are automatically created on first execution
* Table: assets
* Columns
    * hash : SHA1 hash of file url;
    * url : Full file path
    * date: upload date
    
 
* Inspection example:
    * $ sqlite3 sqlite.db
    * $ select * from assets;

