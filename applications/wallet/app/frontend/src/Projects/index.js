import PropTypes from 'prop-types'
import useSWR from 'swr'

import userShape from '../User/shape'

import ERROR_COMPONENT from '../ERROR_COMPONENT'
import Layout from '../Layout'

const Projects = ({ user, logout, children }) => {
  const { data: { results } = {}, error } = useSWR('/api/v1/projects/')

  if (error) {
    return <ERROR_COMPONENT error="Error!" />
  }

  if (!Array.isArray(results)) return 'Loading...'

  if (results.length === 0) return 'You have 0 projects'

  return (
    <Layout user={user} logout={logout} results={results}>
      {children}
    </Layout>
  )
}

Projects.propTypes = {
  user: PropTypes.shape(userShape).isRequired,
  logout: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default Projects
