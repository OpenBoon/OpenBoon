import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

const Projects = ({ children }) => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: { results: projects } = {} } = useSWR('/api/v1/projects/')

  if (!Array.isArray(projects)) {
    return (
      <div css={{ padding: spacing.normal, paddingLeft: 0 }}>Loading...</div>
    )
  }

  if (projects.length === 0) {
    if (projectId) {
      Router.push('/')

      return null
    }

    return children
  }

  if (!projectId || !projects.find(({ id }) => projectId === id)) {
    Router.push('/[projectId]/jobs', `/${projects[0].id}/jobs`)

    return null
  }

  return children
}

Projects.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Projects
