import PropTypes from 'prop-types'

const projectShape = PropTypes.shape({
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  url: PropTypes.string,
  jobs: PropTypes.string,
  apikeys: PropTypes.string,
  assets: PropTypes.string,
  users: PropTypes.string,
  permissions: PropTypes.string,
  tasks: PropTypes.string,
  task_errors: PropTypes.string,
  datasources: PropTypes.string,
})

export default projectShape
