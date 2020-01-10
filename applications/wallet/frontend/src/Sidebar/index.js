import { forwardRef, useEffect } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { colors, spacing, zIndex, constants } from '../Styles'

import SidebarLink from './Link'
import SidebarOverlay from './Overlay'

import QueueSvg from './icons/queue.svg'
import KeySvg from './icons/key.svg'

const WIDTH = 240
const ICON_WIDTH = 20

const Sidebar = forwardRef(({ isSidebarOpen, setSidebarOpen }, ref) => {
  useEffect(() => {
    const handleRouteChange = () => {
      setSidebarOpen(false)
    }

    Router.events.on('routeChangeStart', handleRouteChange)

    return () => {
      Router.events.off('routeChangeStart', handleRouteChange)
    }
  }, [setSidebarOpen])

  return (
    <div>
      <nav
        ref={ref}
        css={{
          width: WIDTH,
          position: 'fixed',
          height: `calc(100% - ${constants.navbar.height}px)`,
          overflowY: 'auto',
          zIndex: zIndex.layout.drawer,
          backgroundColor: colors.structure.iron,
          paddingBottom: spacing.spacious,
          transition: 'left ease-in-out .3s, visibility ease-in-out .3s',
          overscrollBehavior: 'contain',
          left: isSidebarOpen ? 0 : -WIDTH,
          top: constants.navbar.height,
          paddingTop: spacing.moderate,
        }}>
        <ul
          css={{
            listStyleType: 'none',
            padding: 0,
            margin: 0,
          }}>
          <SidebarLink href="/jobs">
            <QueueSvg width={ICON_WIDTH} aria-hidden />
            Job Queue
          </SidebarLink>

          <SidebarLink href="/api-keys">
            <KeySvg width={ICON_WIDTH} aria-hidden />
            API Keys
          </SidebarLink>
        </ul>
      </nav>
      <SidebarOverlay
        isSidebarOpen={isSidebarOpen}
        setSidebarOpen={setSidebarOpen}
      />
    </div>
  )
})

Sidebar.propTypes = {
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
}

export default Sidebar
