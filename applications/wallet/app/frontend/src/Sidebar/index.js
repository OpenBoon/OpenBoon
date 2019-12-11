import { forwardRef, useEffect } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, zIndex, constants, typography } from '../Styles'

import SidebarOverlay from './Overlay'

import QueueSvg from './icons/queue.svg'
import KeySvg from './icons/key.svg'
import { closeSidebar } from './helpers'

const WIDTH = 240
const ICON_WIDTH = 20

const Sidebar = forwardRef(({ isSidebarOpen, setSidebarOpen }, ref) => {
  useEffect(closeSidebar({ setSidebarOpen }), [setSidebarOpen])

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
          backgroundColor: colors.grey5,
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
            a: {
              display: 'flex',
              alignItems: 'center',
              padding: spacing.moderate,
              fontSize: typography.size.kilo,
              color: colors.grey2,
              svg: {
                marginRight: spacing.moderate,
              },
              ':hover': {
                textDecoration: 'none',
                color: colors.plants.clover,
                backgroundColor: colors.grey1,
              },
            },
          }}>
          <li>
            <Link href="/">
              <a>
                <QueueSvg width={ICON_WIDTH} aria-hidden />
                Data Queue
              </a>
            </Link>
          </li>
          <li>
            <Link href="/api-keys">
              <a>
                <KeySvg width={ICON_WIDTH} aria-hidden />
                API Keys
              </a>
            </Link>
          </li>
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
