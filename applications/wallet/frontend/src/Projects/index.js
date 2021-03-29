import { useEffect } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR, { mutate } from 'swr'

import ProjectBoundary from '../ProjectBoundary'

const NO_PROJECT_ID_ROUTES = [
  '/icons',
  '/account',
  '/account/password',
  '/organizations',
  '/organizations/[organizationId]',
  '/organizations/[organizationId]/users',
  '/organizations/[organizationId]/owners',
]

const Projects = ({ projectId, children }) => {
  const { query: { projectId: routerProjectId } = {}, pathname } = useRouter()

  const {
    data: { results: projects },
  } = useSWR('/api/v1/projects/')

  useEffect(() => {
    if (!routerProjectId) return

    if (projectId === routerProjectId) return

    mutate(
      '/api/v1/me/',
      (user) => ({ ...user, projectId: routerProjectId }),
      false,
    )
  }, [projectId, routerProjectId])

  // Render "Account Overview"
  if (pathname === '/') {
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

  // Check if projectId is part of current projects
  if (!projects.find(({ id }) => routerProjectId === id)) {
    return <ProjectBoundary />
  }

  return children
}

Projects.propTypes = {
  projectId: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default Projects
