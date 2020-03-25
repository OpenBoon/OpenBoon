import PropTypes from 'prop-types'

const projectShape = PropTypes.shape({
  id: PropTypes.string.isRequired,
  url: PropTypes.string.isRequired,
  jobs: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  users: PropTypes.arrayOf(PropTypes.string.isRequired),
})

export default projectShape
