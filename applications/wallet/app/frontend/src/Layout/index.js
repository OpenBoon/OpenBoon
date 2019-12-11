import { useState, useEffect, useRef } from 'react'
import PropTypes from 'prop-types'
import {
  disableBodyScroll,
  enableBodyScroll,
  clearAllBodyScrollLocks,
} from 'body-scroll-lock'

import mockProjects from '../ProjectSwitcher/__mocks__/projects'

import { constants } from '../Styles'

import Sidebar from '../Sidebar'

import LayoutNavBar from './NavBar'

const Layout = ({ logout, children }) => {
  const results = mockProjects.list

  const sidebarRef = useRef()

  const [isSidebarOpen, setSidebarOpen] = useState(false)

  const [selectedProject, setSelectedProject] = useState({
    id: results[0].id,
    name: results[0].name,
  })

  useEffect(() => {
    if (isSidebarOpen) disableBodyScroll(sidebarRef.current)
    if (!isSidebarOpen) enableBodyScroll(sidebarRef.current)
    return clearAllBodyScrollLocks
  }, [isSidebarOpen, sidebarRef])

  const projects = results.map(({ id, name }) => {
    return { id, name, selected: selectedProject.id === id }
  })

  return (
    <div css={{ height: '100%' }}>
      <LayoutNavBar
        isSidebarOpen={isSidebarOpen}
        projects={projects}
        setSidebarOpen={setSidebarOpen}
        setSelectedProject={setSelectedProject}
        logout={logout}
      />
      <Sidebar
        isSidebarOpen={isSidebarOpen}
        setSidebarOpen={setSidebarOpen}
        ref={sidebarRef}
      />
      <div css={{ paddingTop: constants.navbar.height }}>
        {children({ selectedProject })}
      </div>
    </div>
  )
}

Layout.propTypes = {
  logout: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default Layout
