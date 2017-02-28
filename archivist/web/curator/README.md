# Curator JS Install Instructions

In the zorroa-js-curator repository, build a deployable version of curator.

```
rm -rf node_modules &&  npm install && npm run build
```

Note: To skip unit test for curator use:

```
npm run build-only
```

Then copy the files from 'bin' into this directory:

```
cp -r bin/* zorroa-server/archivist/web/curator
```
