import PropTypes from 'prop-types'
import { useState } from 'react'

import Navbar from '../Navbar'

const Layout = ({ results, children }) => {
  const [selectedProject, setSelectedProject] = useState({
    id: results[0].url,
    name: results[0].name,
  })
  const projects = results.map(({ url, name }) => {
    return { id: url, name, selected: selectedProject.id === url }
  })

  return (
    <div css={{ height: '100%' }}>
      <Navbar projects={projects} setSelectedProject={setSelectedProject} />
      {children(selectedProject)}
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
  children: PropTypes.func.isRequired,
}

export default Layout
