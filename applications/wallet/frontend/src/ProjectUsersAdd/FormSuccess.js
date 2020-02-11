import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import FormError from '../FormError'
import FormSuccess from '../FormSuccess'
import Button, { VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import SectionTitle from '../SectionTitle'

const ProjectUsersAddFormSuccess = ({
  projectId,
  usersAdded,
  usersNeedAccount,
  permissions,
  onReset,
}) => {
  return (
    <div>
      <FormError>Users Not Added!</FormError>

      <SectionTitle>Users that Need an Account</SectionTitle>
      <div
        css={{
          paddingTop: spacing.moderate,
          color: colors.structure.steel,
        }}>
        {usersNeedAccount.join(', ')}
      </div>

      <FormSuccess>Users Added!</FormSuccess>

      <SectionTitle>Users Added</SectionTitle>
      <div
        css={{
          paddingTop: spacing.moderate,
          color: colors.structure.steel,
        }}>
        {usersAdded.join(', ')}
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
        <Link
          href="/[projectId]/users"
          as={'/[projectId]/users'.replace('[projectId]', projectId)}
          passHref>
          <Button variant={VARIANTS.PRIMARY}>View All</Button>
        </Link>
      </ButtonGroup>
    </div>
  )
}

ProjectUsersAddFormSuccess.propTypes = {
  projectId: PropTypes.string.isRequired,
  usersAdded: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  usersNeedAccount: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  onReset: PropTypes.func.isRequired,
}

export default ProjectUsersAddFormSuccess
