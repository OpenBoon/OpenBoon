import { forwardRef, useEffect } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { colors, spacing, zIndex, constants } from '../Styles'

import BetaBadge from '../BetaBadge'

import DashboardSvg from '../Icons/dashboard.svg'
import DataSourcesSvg from '../Icons/datasources.svg'
import JobQueueSvg from '../Icons/jobQueue.svg'
import ModelsSvg from '../Icons/models.svg'
import VisualizerSvg from '../Icons/visualizer.svg'
import KeySvg from '../Icons/key.svg'
import UsersSvg from '../Icons/users.svg'
import GearSvg from '../Icons/gear.svg'

import SidebarLink from './Link'
import SidebarOverlay from './Overlay'

const WIDTH = 240

const Sidebar = forwardRef(
  ({ projectId, isSidebarOpen, setSidebarOpen }, ref) => {
    useEffect(() => {
      const handleRouteChange = () => {
        setSidebarOpen(false)
      }

      Router.events.on('routeChangeStart', handleRouteChange)

      return () => {
        Router.events.off('routeChangeStart', handleRouteChange)
      }
    }, [setSidebarOpen])

    if (!projectId) return null

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
            transition: 'left ease-in-out .3s, visibility ease-in-out .3s',
            overscrollBehavior: 'contain',
            left: isSidebarOpen ? 0 : -WIDTH,
            top: constants.navbar.height,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <ul
            css={{
              listStyleType: 'none',
              padding: 0,
              margin: 0,
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
            }}
          >
            <SidebarLink projectId={projectId} href="/[projectId]">
              <DashboardSvg height={constants.icons.regular} />
              Project Dashboard
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/data-sources">
              <DataSourcesSvg height={constants.icons.regular} />
              Data Sources
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/jobs">
              <JobQueueSvg height={constants.icons.regular} />
              Job Queue
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/models">
              <ModelsSvg height={constants.icons.regular} />
              Custom Models
              <BetaBadge />
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/visualizer">
              <VisualizerSvg height={constants.icons.regular} />
              Visualizer
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/api-keys">
              <KeySvg height={constants.icons.regular} />
              API Keys
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/users">
              <UsersSvg height={constants.icons.regular} />
              User Admin
            </SidebarLink>
          </ul>
          <ul
            css={{
              listStyleType: 'none',
              padding: 0,
              margin: 0,
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
              backgroundColor: colors.structure.smoke,
            }}
          >
            <SidebarLink projectId={projectId} href="/">
              <DashboardSvg height={constants.icons.regular} />
              Account Dashboard
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/account">
              <GearSvg height={constants.icons.regular} />
              Account
            </SidebarLink>
          </ul>
        </nav>
        <SidebarOverlay
          isSidebarOpen={isSidebarOpen}
          setSidebarOpen={setSidebarOpen}
        />
      </div>
    )
  },
)

Sidebar.propTypes = {
  projectId: PropTypes.string.isRequired,
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
}

export default Sidebar
