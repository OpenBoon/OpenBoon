import PropTypes from 'prop-types'

import { zIndex, colors } from '../Styles'

const SidebarOverlay = ({ isSidebarOpen, setSidebarOpen }) => (
  <div
    role="button"
    aria-label="Close Sidebar Menu"
    tabIndex="-1"
    css={{
      position: 'fixed',
      top: 0,
      left: 0,
      bottom: 0,
      zIndex: zIndex.layout.overlay,
      backgroundColor: colors.structure.black,
      opacity: isSidebarOpen ? 0.5 : 0,
      width: isSidebarOpen ? '100%' : 0,
      transition: isSidebarOpen
        ? 'opacity ease-in-out .3s, width ease-in 0s 0s'
        : 'opacity ease-in-out .3s, width ease-in 0s .3s',
    }}
    onClick={() => setSidebarOpen(false)}
    onKeyDown={() => setSidebarOpen(false)}
  />
)

SidebarOverlay.propTypes = {
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
}

export default SidebarOverlay
