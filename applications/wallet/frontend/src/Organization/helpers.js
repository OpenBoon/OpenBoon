export const getTitle = ({ pathname }) => {
  if (pathname === '/organizations/[organizationId]/owners') {
    return 'Owners'
  }

  if (pathname === '/organizations/[organizationId]/users') {
    return 'Users'
  }

  return 'Projects'
}
