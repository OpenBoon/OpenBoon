import { useState, useEffect, useRef } from 'react'
import PropTypes from 'prop-types'
import {
  disableBodyScroll,
  enableBodyScroll,
  clearAllBodyScrollLocks,
} from 'body-scroll-lock'

import userShape from '../User/shape'

import { constants, spacing } from '../Styles'

import Navbar from '../Navbar'
import Sidebar from '../Sidebar'

const Layout = ({ user, logout, children }) => {
  const sidebarRef = useRef()

  const [isSidebarOpen, setSidebarOpen] = useState(false)

  useEffect(() => {
    if (isSidebarOpen) disableBodyScroll(sidebarRef.current)
    if (!isSidebarOpen) enableBodyScroll(sidebarRef.current)
    return clearAllBodyScrollLocks
  }, [isSidebarOpen, sidebarRef])

  return (
    <div css={{ height: '100%' }}>
      <Navbar
        user={user}
        isSidebarOpen={isSidebarOpen}
        setSidebarOpen={setSidebarOpen}
        logout={logout}
      />
      <Sidebar
        isSidebarOpen={isSidebarOpen}
        setSidebarOpen={setSidebarOpen}
        ref={sidebarRef}
      />
      <div
        css={{
          marginTop: constants.navbar.height,
          paddingLeft: spacing.spacious,
          paddingRight: spacing.spacious,
          paddingBottom: spacing.spacious,
          height: `calc(100vh - ${constants.navbar.height}px)`,
          display: 'flex',
          flexDirection: 'column',
        }}>
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
