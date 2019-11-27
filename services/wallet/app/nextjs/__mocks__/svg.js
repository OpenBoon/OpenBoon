const path = require('path')

module.exports = {
  process(src, filename) {
    const name = path.basename(filename)
    return `
      const React = require('react');
      module.exports = (props) => React.createElement('svg',  { ...props, 'data-file-name': '${name}' });
    `
  },
}
