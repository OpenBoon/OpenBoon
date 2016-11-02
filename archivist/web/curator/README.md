# Curator JS Install Instructions

In the zorroa-js-curator repository, build a deployable version of curator.

```
rm -rf node_modules &&  npm install && npm run build
```

Then copy the files from 'bin' into this directory:

```
cp -r bin/* zorroa-server/archivist/web/curator
```
