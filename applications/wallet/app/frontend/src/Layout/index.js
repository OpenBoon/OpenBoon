import PropTypes from 'prop-types'
import { useState } from 'react'

import LayoutNavBar from './NavBar'
import mockProjects from '../ProjectSwitcher/__mocks__/projects'

const Layout = ({ children }) => {
  const results = mockProjects.list

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
        projects={projects}
        setSelectedProject={setSelectedProject}
      />
      {children({ selectedProject })}
    </div>
  )
}

Layout.propTypes = {
  children: PropTypes.func.isRequired,
}

export default Layout
