import { forwardRef, useEffect } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'
import Router, { useRouter } from 'next/router'

import { colors, spacing, zIndex, constants, typography } from '../Styles'

import SidebarOverlay from './Overlay'

import QueueSvg from './icons/queue.svg'
import KeySvg from './icons/key.svg'

const WIDTH = 240
const ICON_WIDTH = 20

const Sidebar = forwardRef(({ isSidebarOpen, setSidebarOpen }, ref) => {
  const { pathname } = useRouter()
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
            li: {
              borderBottom: constants.borders.transparent,
            },
            a: {
              display: 'flex',
              alignItems: 'center',
              padding: spacing.moderate,
              fontSize: typography.size.kilo,
              svg: {
                marginRight: spacing.moderate,
              },
              ':hover': {
                textDecoration: 'none',
                color: colors.structure.pebble,
                backgroundColor: colors.structure.smoke,
              },
            },
          }}>
          <li>
            <Link href="/">
              <a
                css={{
                  backgroundColor:
                    pathname === '/' ? colors.structure.smoke : 'none',
                  color:
                    pathname === '/'
                      ? colors.plants.clover
                      : colors.structure.steel,
                }}>
                <QueueSvg width={ICON_WIDTH} aria-hidden />
                Job Queue
              </a>
            </Link>
          </li>
          <li>
            <Link href="/api-keys">
              <a
                css={{
                  backgroundColor:
                    pathname === '/api-keys' ? colors.structure.smoke : 'none',
                  color:
                    pathname === '/api-keys'
                      ? colors.plants.clover
                      : colors.structure.steel,
                }}>
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
