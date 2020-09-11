import PropTypes from 'prop-types'

const videoShape = PropTypes.shape({
  play: PropTypes.func,
  pause: PropTypes.func,
  addEventListener: PropTypes.func,
  removeEventListener: PropTypes.func,
  currentTime: PropTypes.number,
  duration: PropTypes.number,
  paused: PropTypes.bool,
})

export default videoShape
