import React from 'react'
import PropTypes from 'prop-types'

function Logo({ width, height }) {
  return (
    <img
      width={width}
      height={height}
      className="Logo"
      src={require('../../assets/images/logo.svg')}
    />
  )
}

Logo.propTypes = {
  width: PropTypes.string,
  height: PropTypes.string,
}

Logo.defaultProps = {
  width: '250',
  height: '50',
}

export default Logo
