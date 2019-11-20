import React, { useState, useRef } from 'react'
import PropTypes from 'prop-types'

function Logo({ width, height }) {
  return (
    <img width={width} height={height} className="Logo" src={require('../../images/logo.svg')} />
  )
}

Logo.propTypes = {
  width: PropTypes.number,
  height: PropTypes.number,
}

export default Logo