import PropTypes from 'prop-types'
import useSWR from 'swr'

import Layout from '../Layout'

const Projects = ({ logout, children }) => {
  const { data: { results } = {} } = useSWR('api/v1/projects/')

  if (!Array.isArray(results)) return 'Loading...'

  if (results.length === 0) return 'You have 0 projects'

  return (
    <Layout logout={logout} results={results}>
      {children}
    </Layout>
  )
}

Projects.propTypes = {
  logout: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default Projects
