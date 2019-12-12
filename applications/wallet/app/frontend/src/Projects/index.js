import PropTypes from 'prop-types'
import useSWR from 'swr'

import Layout from '../Layout'

const Projects = ({ logout, children }) => {
  const { data: { results = [] } = {} } = useSWR('api/v1/projects/')

  if (results.length === 0) return 'Loading...'

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
