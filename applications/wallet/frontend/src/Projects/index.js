import { useEffect } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'

const NO_PROJECT_ID_ROUTES = ['/account', '/account/password']

const Projects = ({ projectId, setUser, children }) => {
  const {
    query: { projectId: routerProjectId },
    pathname,
  } = useRouter()

  const { data: { results: projects } = {} } = useSWR('/api/v1/projects/')

  useEffect(() => {
    if (!routerProjectId) return

    if (projectId === routerProjectId) return

    setUser({ user: { projectId: routerProjectId } })
  }, [projectId, routerProjectId, setUser])

  if (!Array.isArray(projects)) return <Loading />

  if (projects.length === 0) {
    if (routerProjectId) {
      Router.push('/')

      return null
    }

    return children
  }

  if (NO_PROJECT_ID_ROUTES.includes(pathname)) return children

  if (!routerProjectId || !projects.find(({ id }) => routerProjectId === id)) {
    Router.push('/[projectId]/jobs', `/${projectId || projects[0].id}/jobs`)

    return null
  }

  return children
}

Projects.propTypes = {
  projectId: PropTypes.string.isRequired,
  setUser: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Projects
