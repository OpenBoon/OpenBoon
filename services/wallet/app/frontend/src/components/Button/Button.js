import React from 'react'
import PropTypes from 'prop-types'

function Button({ value }) {
  return (
    <div className={`Button Button__${value}`}>{value}</div>
  )
}

Button.propTypes = {
  status: PropTypes.oneOf(['Active', 'Paused', 'Canceled', 'Finished'])
}

export default Button