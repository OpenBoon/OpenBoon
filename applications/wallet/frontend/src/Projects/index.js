import { useEffect } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'

const NO_PROJECT_ID_ROUTES = ['/account', '/account/password']

const Projects = ({ projectId, setUser, children }) => {
  const { query: { projectId: routerProjectId } = {}, pathname } = useRouter()

  const {
    data: { results: projects },
  } = useSWR('/api/v1/projects/')

  useEffect(() => {
    if (!routerProjectId) return

    if (projectId === routerProjectId) return

    setUser({ user: { projectId: routerProjectId } })
  }, [projectId, routerProjectId, setUser])

  // Reset user projectId if not part of current projects
  if (projectId && !projects.find(({ id }) => projectId === id)) {
    setUser({ user: { projectId: '' } })
    return null
  }

  // Render "No Projects" page
  if (projects.length === 0 && pathname === '/') {
    return children
  }

  // Render other pages without projectId
  if (!routerProjectId && NO_PROJECT_ID_ROUTES.includes(pathname)) {
    return children
  }

  // Redirect pages that require a projectId to "No Projects" page if there are no projects
  if (projects.length === 0 && !NO_PROJECT_ID_ROUTES.includes(pathname)) {
    Router.push('/')
    return null
  }

  // Remove url projectId if not part of current projects
  if (!projects.find(({ id }) => routerProjectId === id)) {
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
