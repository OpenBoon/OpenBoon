import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import SectionTitle from '../SectionTitle'

import ProjectUsersAddCopyLink from './CopyLink'

const ProjectUsersAddFormResponse = ({
  projectId,
  succeeded,
  failed,
  roles: r,
  onReset,
}) => {
  const roles = Object.keys(r).filter((name) => r[name])

  return (
    <div>
      {failed.length > 0 && (
        <>
          <div
            css={{
              display: 'flex',
              paddingTop: spacing.base,
              paddingBottom: spacing.base,
            }}
          >
            <FlashMessage variant={FLASH_VARIANTS.ERROR}>
              Users not added.
            </FlashMessage>
          </div>
          <SectionTitle>Users that Need an Account</SectionTitle>
          <div
            css={{
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
              color: colors.structure.steel,
            }}
          >
            {failed.map((user) => user.email).join(', ')}
          </div>

          <ProjectUsersAddCopyLink />
        </>
      )}

      {succeeded.length > 0 && (
        <>
          <div
            css={{
              display: 'flex',
              paddingTop: spacing.base,
              paddingBottom: spacing.base,
            }}
          >
            <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
              Users added.
            </FlashMessage>
          </div>
          <SectionTitle>Users Added</SectionTitle>
          <div
            css={{
              paddingTop: spacing.moderate,
              color: colors.structure.steel,
            }}
          >
            {succeeded.map((user) => user.email).join(', ')}
          </div>

          <SectionTitle>Roles</SectionTitle>
          <ul css={{ color: colors.structure.zinc }}>
            {roles.map((role) => (
              <li css={{ fontWeight: typography.weight.bold }} key={role}>
                {role.replaceAll('_', ' ')}
              </li>
            ))}
          </ul>

          <ButtonGroup>
            <Button variant={BUTTON_VARIANTS.SECONDARY} onClick={onReset}>
              Add Another User
            </Button>
            <Link href="/[projectId]/users" as={`/${projectId}/users`} passHref>
              <Button variant={BUTTON_VARIANTS.PRIMARY}>View All</Button>
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
      roles: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    }).isRequired,
  ).isRequired,
  failed: PropTypes.arrayOf(
    PropTypes.shape({
      email: PropTypes.string.isRequired,
      roles: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    }).isRequired,
  ).isRequired,
  roles: PropTypes.objectOf(PropTypes.bool.isRequired).isRequired,
  onReset: PropTypes.func.isRequired,
}

export default ProjectUsersAddFormResponse
