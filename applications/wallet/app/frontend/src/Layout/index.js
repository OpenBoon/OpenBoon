import { useState } from 'react'
import PropTypes from 'prop-types'

import mockProjects from '../ProjectSwitcher/__mocks__/projects'

import { constants } from '../Styles'

import Sidebar from '../Sidebar'

import LayoutNavBar from './NavBar'

const Layout = ({ children }) => {
  const results = mockProjects.list

  const [isSidebarOpen, setSidebarOpen] = useState(false)

  const [selectedProject, setSelectedProject] = useState({
    id: results[0].id,
    name: results[0].name,
  })

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
      />
      <Sidebar isSidebarOpen={isSidebarOpen} setSidebarOpen={setSidebarOpen} />
      <div css={{ paddingTop: constants.navbar.height }}>
        {children({ selectedProject })}
      </div>
    </div>
  )
}

Layout.propTypes = {
  children: PropTypes.func.isRequired,
}

export default Layout
