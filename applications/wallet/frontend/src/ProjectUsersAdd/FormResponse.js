import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import FormError from '../FormError'
import FormSuccess from '../FormSuccess'
import Button, { VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import SectionTitle from '../SectionTitle'

const ProjectUsersAddFormResponse = ({
  projectId,
  succeeded,
  failed,
  permissions: p,
  onReset,
}) => {
  const permissions = Object.keys(p).filter(name => p[name])

  return (
    <div>
      {failed.length > 0 && (
        <>
          <FormError>Users Not Added!</FormError>
          <SectionTitle>Users that Need an Account</SectionTitle>
          <div
            css={{
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
              color: colors.structure.steel,
            }}>
            {failed.map(user => user.email).join(', ')}
          </div>
        </>
      )}

      {succeeded.length > 0 && (
        <>
          <FormSuccess>Users Added!</FormSuccess>

          <SectionTitle>Users Added</SectionTitle>
          <div
            css={{
              paddingTop: spacing.moderate,
              color: colors.structure.steel,
            }}>
            {succeeded.map(user => user.email).join(', ')}
          </div>

          <SectionTitle>Permissions</SectionTitle>
          <ul css={{ color: colors.structure.zinc }}>
            {permissions.map(permission => (
              <li css={{ fontWeight: typography.weight.bold }} key={permission}>
                {permission.replace(/([A-Z])/g, match => ` ${match}`)}
              </li>
            ))}
          </ul>

          <ButtonGroup>
            <Button variant={VARIANTS.SECONDARY} onClick={onReset}>
              Add Another User
            </Button>
            <Link href="/[projectId]/users" as={`/${projectId}/users`} passHref>
              <Button variant={VARIANTS.PRIMARY}>View All</Button>
            </Link>
          </ButtonGroup>
        </>
      )}
    </div>
  )
}

ProjectUsersAddFormResponse.propTypes = {
  projectId: PropTypes.string.isRequired,
  succeeded: PropTypes.arrayOf(
    PropTypes.shape({
      email: PropTypes.string.isRequired,
      permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    }).isRequired,
  ).isRequired,
  failed: PropTypes.arrayOf(
    PropTypes.shape({
      email: PropTypes.string.isRequired,
      permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    }).isRequired,
  ).isRequired,
  permissions: PropTypes.objectOf(PropTypes.bool.isRequired).isRequired,
  onReset: PropTypes.func.isRequired,
}

export default ProjectUsersAddFormResponse
