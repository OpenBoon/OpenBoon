import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'

const Projects = ({ children }) => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: { results: projects } = {} } = useSWR('/api/v1/projects/')

  if (!Array.isArray(projects)) return 'Loading...'

  if (projects.length === 0) return 'You have 0 projects'

  if (!projectId || !projects.find(({ id }) => projectId === id)) {
    Router.push('/[projectId]/jobs', `/${projects[0].id}/jobs`)

    return null
  }

  return children({ projectId })
}

Projects.propTypes = {
  children: PropTypes.func.isRequired,
}

export default Projects
