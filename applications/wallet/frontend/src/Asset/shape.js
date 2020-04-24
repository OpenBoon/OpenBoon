import PropTypes from 'prop-types'

const assetShape = PropTypes.shape({
  id: PropTypes.string.isRequired,
  url: PropTypes.string.isRequired,
  metadata: PropTypes.shape({
    source: PropTypes.shape({
      path: PropTypes.string,
      filename: PropTypes.string,
      extension: PropTypes.string,
      mimetype: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
    system: PropTypes.shape({
      projectId: PropTypes.string,
      dataSourceId: PropTypes.string,
      jobId: PropTypes.string,
      taskId: PropTypes.string,
      timeCreated: PropTypes.string,
      state: PropTypes.string,
    }).isRequired,
    files: PropTypes.arrayOf(
      PropTypes.shape({
        size: PropTypes.number,
        name: PropTypes.string,
        mimetype: PropTypes.string,
        category: PropTypes.oneOf(['proxy', 'source', 'web-proxy']),
        attrs: PropTypes.shape({
          width: PropTypes.number,
          height: PropTypes.number,
        }),
        url: PropTypes.string,
      }),
    ).isRequired,
  }).isRequired,
})

export default assetShape
