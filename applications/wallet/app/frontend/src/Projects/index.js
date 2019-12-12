import PropTypes from 'prop-types'
import useSWR from 'swr'

import Layout from '../Layout'

const Projects = ({ children }) => {
  const { data: { results = [] } = {} } = useSWR('api/v1/projects/')

  if (results.length === 0) return 'Loading...'

  return <Layout results={results}>{children}</Layout>
}

Projects.propTypes = {
  children: PropTypes.func.isRequired,
}

export default Projects
