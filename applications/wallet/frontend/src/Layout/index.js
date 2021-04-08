import { useState, useEffect, useRef } from 'react'
import PropTypes from 'prop-types'
import {
  disableBodyScroll,
  enableBodyScroll,
  clearAllBodyScrollLocks,
} from 'body-scroll-lock'
import { SkipNavLink, SkipNavContent } from '@reach/skip-nav'

import userShape from '../User/shape'

import { colors, constants, spacing, typography, zIndex } from '../Styles'

import Navbar from '../Navbar'
import UserMenu from '../UserMenu'
import Sidebar from '../Sidebar'

const Layout = ({ user, logout, children }) => {
  const sidebarRef = useRef()

  const [isSidebarOpen, setSidebarOpen] = useState(false)

  useEffect(() => {
    if (isSidebarOpen) disableBodyScroll(sidebarRef.current || true)
    if (!isSidebarOpen) enableBodyScroll(sidebarRef.current || true)
    return clearAllBodyScrollLocks
  }, [isSidebarOpen, sidebarRef])

  return (
    <div css={{ height: '100%' }}>
      <SkipNavLink
        css={{
          ':focus': {
            backgroundColor: colors.structure.smoke,
            boxShadow: constants.boxShadows.default,
            zIndex: zIndex.layout.navbar + 1,
            borderRadius: constants.borderRadius.small,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            fontWeight: typography.weight.medium,
          },
        }}
      />

      <Navbar
        projectId={user.projectId}
        isSidebarOpen={isSidebarOpen}
        setSidebarOpen={setSidebarOpen}
      >
        <UserMenu user={user} logout={logout} />
      </Navbar>

      <Sidebar
        projectId={user.projectId}
        user={user}
        isSidebarOpen={isSidebarOpen}
        setSidebarOpen={setSidebarOpen}
        ref={sidebarRef}
      />

      <SkipNavContent />

      <div
        css={{
          marginTop: constants.navbar.height,
          paddingLeft: spacing.spacious,
          paddingRight: spacing.spacious,
          paddingBottom: spacing.spacious,
          height: `calc(100vh - ${constants.navbar.height}px)`,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {children}
      </div>
    </div>
  )
}

Layout.propTypes = {
  user: PropTypes.shape(userShape).isRequired,
  logout: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Layout
