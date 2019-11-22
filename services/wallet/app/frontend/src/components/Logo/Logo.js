import React from 'react'
import PropTypes from 'prop-types'

import LogoSrc from '../../assets/images/logo.svg'

function Logo({ width, height }) {
  return (
    <img
      width={width}
      height={height}
      className="Logo"
      src={LogoSrc}
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
