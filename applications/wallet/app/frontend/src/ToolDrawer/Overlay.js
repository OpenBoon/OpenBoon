import React from 'react'
import PropTypes from 'prop-types'
import { css } from '@emotion/core'

import { zIndex, colors } from '../Styles'

const ToolDrawerOverlay = ({ isToolDrawerOpen, setToolDrawerOpen }) => (
  <div
    role="button"
    aria-label="Close Tool Drawer"
    tabIndex="-1"
    css={css({
      position: 'fixed',
      top: 0,
      left: 0,
      bottom: 0,
      zIndex: zIndex.layout.overlay,
      backgroundColor: colors.black,
      opacity: isToolDrawerOpen ? 0.5 : 0,
      width: isToolDrawerOpen ? '100%' : 0,
      transition: isToolDrawerOpen
        ? 'opacity ease-in-out .3s, width ease-in 0s 0s'
        : 'opacity ease-in-out .3s, width ease-in 0s .3s',
    })}
    onClick={() => setToolDrawerOpen(false)}
    onKeyDown={() => setToolDrawerOpen(false)}
  />
)

ToolDrawerOverlay.propTypes = {
  isToolDrawerOpen: PropTypes.bool.isRequired,
  setToolDrawerOpen: PropTypes.func.isRequired,
}

export default ToolDrawerOverlay
