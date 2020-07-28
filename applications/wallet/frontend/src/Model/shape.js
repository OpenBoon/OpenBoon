import PropTypes from 'prop-types'

const modelShape = PropTypes.shape({
  id: PropTypes.string,
  name: PropTypes.string,
  type: PropTypes.string,
  moduleName: PropTypes.string,
  fileId: PropTypes.string,
  trainingJobName: PropTypes.string,
  ready: PropTypes.bool,
  deploySearch: PropTypes.shape({
    query: PropTypes.shape({
      matchAll: PropTypes.shape({}),
    }),
  }),
  timeCreated: PropTypes.number,
  timeModified: PropTypes.number,
  actorCreated: PropTypes.string,
  actorModified: PropTypes.string,
  url: PropTypes.string,
  runningJobId: PropTypes.string,
})

export default modelShape
