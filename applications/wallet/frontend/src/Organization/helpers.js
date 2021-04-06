export const getTitle = ({ pathname }) => {
  if (pathname === '/organizations/[organizationId]/owners/add') {
    return 'Add Owners'
  }

  if (pathname === '/organizations/[organizationId]/owners') {
    return 'Owners'
  }

  if (pathname === '/organizations/[organizationId]/users') {
    return 'Users'
  }

  if (pathname === '/organizations/[organizationId]/projects/add') {
    return 'Create Project'
  }

  return 'Projects'
}
