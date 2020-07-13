import PropTypes from 'prop-types'

const listboxShape = PropTypes.objectOf(
  PropTypes.oneOfType([
    PropTypes.objectOf(PropTypes.shape({})),
    PropTypes.objectOf(PropTypes.string),
    PropTypes.string,
  ]),
)

export default listboxShape
