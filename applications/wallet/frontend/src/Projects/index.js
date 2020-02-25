import { useEffect } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'

const NO_PROJECT_ID_ROUTES = ['/account', '/account/password']

const Projects = ({ userProjectId, setUser, children }) => {
  const {
    query: { projectId },
    pathname,
  } = useRouter()

  const { data: { results: projects } = {} } = useSWR('/api/v1/projects/')

  useEffect(() => {
    if (!projectId) return

    if (projectId === userProjectId) return

    setUser({ user: { projectId } })
  }, [projectId, userProjectId, setUser])

  if (!Array.isArray(projects)) return <Loading />

  if (projects.length === 0) {
    if (projectId) {
      Router.push('/')

      return null
    }

    return children
  }

  if (NO_PROJECT_ID_ROUTES.includes(pathname)) return children

  if (!projectId || !projects.find(({ id }) => projectId === id)) {
    Router.push('/[projectId]/jobs', `/${projects[0].id}/jobs`)

    return null
  }

  return children
}

Projects.propTypes = {
  userProjectId: PropTypes.string.isRequired,
  setUser: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Projects
