import PropTypes from 'prop-types'

const assetShape = PropTypes.shape({
  id: PropTypes.string.isRequired,
  metadata: PropTypes.shape({
    source: PropTypes.shape({
      path: PropTypes.string,
      filename: PropTypes.string,
      extension: PropTypes.string,
      mimetype: PropTypes.string,
    }),
    system: PropTypes.shape({
      projectId: PropTypes.string,
      dataSourceId: PropTypes.string,
      jobId: PropTypes.string,
      taskId: PropTypes.string,
      timeCreated: PropTypes.string,
      state: PropTypes.string,
    }),
    files: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.string,
        size: PropTypes.number,
        name: PropTypes.string,
        mimetype: PropTypes.string,
        category: PropTypes.oneOf(['proxy', 'source', 'web-proxy']),
        attrs: PropTypes.shape({
          width: PropTypes.number,
          height: PropTypes.number,
        }),
      }),
    ),
  }).isRequired,
})

export default assetShape
