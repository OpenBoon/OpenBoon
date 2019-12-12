import { useState, useEffect, useRef } from 'react'
import PropTypes from 'prop-types'
import {
  disableBodyScroll,
  enableBodyScroll,
  clearAllBodyScrollLocks,
} from 'body-scroll-lock'

import { constants } from '../Styles'

import Navbar from '../Navbar'
import Sidebar from '../Sidebar'

import { getJobId } from './helpers'

const Layout = ({ results, logout, children }) => {
  const sidebarRef = useRef()

  const [isSidebarOpen, setSidebarOpen] = useState(false)

  const [selectedProject, setSelectedProject] = useState({
    id: getJobId({ url: results[0].url }),
    name: results[0].name,
  })

  const projects = results.map(({ url, name }) => {
    const id = getJobId({ url })

    return {
      id,
      name,
      selected: selectedProject.id === id,
    }
  })

  useEffect(() => {
    if (isSidebarOpen) disableBodyScroll(sidebarRef.current)
    if (!isSidebarOpen) enableBodyScroll(sidebarRef.current)
    return clearAllBodyScrollLocks
  }, [isSidebarOpen, sidebarRef])

  return (
    <div css={{ height: '100%' }}>
      <Navbar
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
  results: PropTypes.arrayOf(
    PropTypes.shape({
      url: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
  logout: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default Layout
