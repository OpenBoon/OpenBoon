import PropTypes from 'prop-types'

const subscriptionShape = PropTypes.shape({
  id: PropTypes.string.isRequired,
  project: PropTypes.string.isRequired,
  limits: PropTypes.shape({
    videoHours: PropTypes.number,
    imageCount: PropTypes.number,
  }),
  usage: PropTypes.shape({
    videoHours: PropTypes.number,
    imageCount: PropTypes.number,
  }),
  modules: PropTypes.arrayOf(PropTypes.string),
  createdDate: PropTypes.string.isRequired,
  modifiedDate: PropTypes.string.isRequired,
  url: PropTypes.string.isRequired,
})

export default subscriptionShape
